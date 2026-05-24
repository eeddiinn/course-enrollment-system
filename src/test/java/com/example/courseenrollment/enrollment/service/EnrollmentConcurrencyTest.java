package com.example.courseenrollment.enrollment.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.enrollment.repository.EnrollmentRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 10명 강의에 9명이 이미 신청한 상태에서 20명이 동시에 신청하면 1명만 성공한다.")
    void createEnrollment_concurrency_lastOneSeat() throws InterruptedException {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 10);

        createExistingEnrollments(course, 9);

        int threadCount = 20;
        int remainingSeats = 1;
        List<User> students = saveStudents("newStudent", threadCount);

        ConcurrencyResult result = runConcurrentEnrollment(students, course.getId());

        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(result.successCount()).isEqualTo(remainingSeats);
        assertThat(result.capacityFullFailCount()).isEqualTo(threadCount - remainingSeats);
        assertThat(result.unexpectedFailCount()).isZero();
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(10);
        assertThat(enrollmentRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("정원 10명 강의에 5명이 이미 신청한 상태에서 20명이 동시에 신청하면 5명만 성공한다.")
    void createEnrollment_concurrency_remainingFiveSeats() throws InterruptedException {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveOpenCourse(creator, "JPA 기초", 10);

        createExistingEnrollments(course, 5);

        int threadCount = 20;
        int remainingSeats = 5;
        List<User> students = saveStudents("newStudent", threadCount);

        ConcurrencyResult result = runConcurrentEnrollment(students, course.getId());

        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(result.successCount()).isEqualTo(remainingSeats);
        assertThat(result.capacityFullFailCount()).isEqualTo(threadCount - remainingSeats);
        assertThat(result.unexpectedFailCount()).isZero();
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(10);
        assertThat(enrollmentRepository.count()).isEqualTo(10);
    }

    private ConcurrencyResult runConcurrentEnrollment(List<User> students, Long courseId) throws InterruptedException {
        int threadCount = students.size();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger capacityFullFailCount = new AtomicInteger();
        AtomicInteger unexpectedFailCount = new AtomicInteger();

        for (User student : students) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    enrollmentService.createEnrollment(student.getId(), courseId);
                    successCount.incrementAndGet();

                } catch (CustomException e) {
                    if (e.getErrorType() == ErrorType.COURSE_CAPACITY_FULL) {
                        capacityFullFailCount.incrementAndGet();
                    } else {
                        unexpectedFailCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    unexpectedFailCount.incrementAndGet();

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        return new ConcurrencyResult(
            successCount.get(),
            capacityFullFailCount.get(),
            unexpectedFailCount.get()
        );
    }

    private void createExistingEnrollments(Course course, int count) {
        for (int i = 0; i < count; i++) {
            User student = saveUser("existingStudent" + i, UserRole.STUDENT);
            enrollmentService.createEnrollment(student.getId(), course.getId());
        }
    }

    private List<User> saveStudents(String prefix, int count) {
        List<User> students = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            students.add(saveUser(prefix + i, UserRole.STUDENT));
        }

        return students;
    }

    private User saveUser(String name, UserRole role) {
        User user = User.builder()
                        .name(name)
                        .role(role)
                        .build();

        return userRepository.save(user);
    }

    private Course saveOpenCourse(User creator, String title, int capacity) {
        Course course = Course.builder()
                            .creator(creator)
                            .title(title)
                            .description(title + " 설명입니다.")
                            .price(50000)
                            .capacity(capacity)
                            .startAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                            .endAt(LocalDateTime.of(2026, 8, 31, 23, 59))
                            .build();

        course.changeStatus(CourseStatus.OPEN);

        return courseRepository.save(course);
    }

    private record ConcurrencyResult(
        int successCount,
        int capacityFullFailCount,
        int unexpectedFailCount
    ) {
    }
}