package com.example.courseenrollment.enrollment.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.enrollment.domain.Enrollment;
import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;
import com.example.courseenrollment.enrollment.dto.CancelEnrollmentResponse;
import com.example.courseenrollment.enrollment.dto.ConfirmEnrollmentResponse;
import com.example.courseenrollment.enrollment.dto.CreateEnrollmentResponse;
import com.example.courseenrollment.enrollment.dto.GetCourseStudentListResponse;
import com.example.courseenrollment.enrollment.dto.GetMyEnrollmentListResponse;
import com.example.courseenrollment.enrollment.repository.EnrollmentRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.global.response.PageResponse;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.enrollment.cancel-available-days}")
    private int cancelAvailableDays;

    @Test
    @DisplayName("STUDENT는 OPEN 상태의 강의에 수강 신청할 수 있다.")
    void createEnrollment_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        CreateEnrollmentResponse response = enrollmentService.createEnrollment(student.getId(), course.getId());

        Enrollment enrollment = enrollmentRepository.findById(response.enrollmentId()).orElseThrow();
        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(response.enrollmentId()).isNotNull();
        assertThat(enrollment.getStudent().getId()).isEqualTo(student.getId());
        assertThat(enrollment.getCourse().getId()).isEqualTo(course.getId());
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 수강 신청할 수 없다.")
    void createEnrollment_fail_whenUserNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(999L, course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("STUDENT가 아닌 사용자는 수강 신청할 수 없다.")
    void createEnrollment_fail_whenUserIsNotStudent() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(creator.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CREATE_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 강의에는 수강 신청할 수 없다.")
    void createEnrollment_fail_whenCourseNotFound() {
        User student = saveUser("student1", UserRole.STUDENT);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(student.getId(), 999L))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 강의에는 수강 신청할 수 없다.")
    void createEnrollment_fail_whenCourseIsNotOpen() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course draftCourse = saveCourse(creator, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(student.getId(), draftCourse.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("이미 PENDING 신청 내역이 있으면 중복 수강 신청할 수 없다.")
    void createEnrollment_fail_whenAlreadyPending() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        enrollmentService.createEnrollment(student.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(student.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ALREADY_ENROLLED);
    }

    @Test
    @DisplayName("이미 CONFIRMED 신청 내역이 있으면 중복 수강 신청할 수 없다.")
    void createEnrollment_fail_whenAlreadyConfirmed() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());
        enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(student.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ALREADY_ENROLLED);
    }

    @Test
    @DisplayName("CANCELLED 신청 내역만 있으면 다시 수강 신청할 수 있다.")
    void createEnrollment_success_whenPreviousEnrollmentCancelled() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        CreateEnrollmentResponse firstResponse = enrollmentService.createEnrollment(student.getId(), course.getId());
        enrollmentService.cancelEnrollment(student.getId(), firstResponse.enrollmentId());

        CreateEnrollmentResponse secondResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        Enrollment secondEnrollment = enrollmentRepository.findById(secondResponse.enrollmentId()).orElseThrow();

        assertThat(secondResponse.enrollmentId()).isNotNull();
        assertThat(secondResponse.enrollmentId()).isNotEqualTo(firstResponse.enrollmentId());
        assertThat(secondEnrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("정원이 마감된 강의에는 수강 신청할 수 없다.")
    void createEnrollment_fail_whenCourseCapacityFull() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student1 = saveUser("student1", UserRole.STUDENT);
        User student2 = saveUser("student2", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 1);

        enrollmentService.createEnrollment(student1.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(student2.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_CAPACITY_FULL);
    }

    @Test
    @DisplayName("PENDING 상태의 수강 신청을 결제 확정할 수 있다.")
    void confirmEnrollment_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        ConfirmEnrollmentResponse response = enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId());

        Enrollment enrollment = enrollmentRepository.findById(response.enrollmentId()).orElseThrow();

        assertThat(response.enrollmentId()).isEqualTo(createResponse.enrollmentId());
        assertThat(response.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenUserNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(999L, createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("STUDENT가 아닌 사용자는 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenUserIsNotStudent() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(creator.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CONFIRM_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 수강 신청은 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenEnrollmentNotFound() {
        User student = saveUser("student1", UserRole.STUDENT);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(student.getId(), 999L))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("본인의 수강 신청이 아니면 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenNotOwner() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student1 = saveUser("student1", UserRole.STUDENT);
        User student2 = saveUser("student2", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student1.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(student2.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CONFIRM_FORBIDDEN);
    }

    @Test
    @DisplayName("CONFIRMED 상태의 수강 신청은 다시 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenAlreadyConfirmed() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CONFIRM_NOT_ALLOWED);
    }

    @Test
    @DisplayName("CANCELLED 상태의 수강 신청은 결제 확정할 수 없다.")
    void confirmEnrollment_fail_whenCancelled() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId());

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CONFIRM_NOT_ALLOWED);
    }

    @Test
    @DisplayName("PENDING 상태의 수강 신청을 취소할 수 있다.")
    void cancelEnrollment_success_whenPending() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        CancelEnrollmentResponse response = enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId());

        Enrollment enrollment = enrollmentRepository.findById(response.enrollmentId()).orElseThrow();
        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelledAt()).isNotNull();
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("CONFIRMED 상태의 수강 신청을 취소 가능 기간 안에 취소할 수 있다.")
    void cancelEnrollment_success_whenConfirmedWithinPeriod() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());
        enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId());

        CancelEnrollmentResponse response = enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId());

        Enrollment enrollment = enrollmentRepository.findById(response.enrollmentId()).orElseThrow();
        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelledAt()).isNotNull();
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 수강 신청을 취소할 수 없다.")
    void cancelEnrollment_fail_whenUserNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(999L, createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("STUDENT가 아닌 사용자는 수강 신청을 취소할 수 없다.")
    void cancelEnrollment_fail_whenUserIsNotStudent() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(creator.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CANCEL_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 수강 신청은 취소할 수 없다.")
    void cancelEnrollment_fail_whenEnrollmentNotFound() {
        User student = saveUser("student1", UserRole.STUDENT);

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(student.getId(), 999L))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("본인의 수강 신청이 아니면 취소할 수 없다.")
    void cancelEnrollment_fail_whenNotOwner() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student1 = saveUser("student1", UserRole.STUDENT);
        User student2 = saveUser("student2", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student1.getId(), course.getId());

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(student2.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CANCEL_FORBIDDEN);
    }

    @Test
    @DisplayName("이미 취소된 수강 신청은 다시 취소할 수 없다.")
    void cancelEnrollment_fail_whenAlreadyCancelled() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());

        enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId());

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("결제 확정 후 취소 가능 기간이 지나면 취소할 수 없다.")
    void cancelEnrollment_fail_whenCancelPeriodExpired() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);
        CreateEnrollmentResponse createResponse = enrollmentService.createEnrollment(student.getId(), course.getId());
        enrollmentService.confirmEnrollment(student.getId(), createResponse.enrollmentId());

        Enrollment enrollment = enrollmentRepository.findById(createResponse.enrollmentId()).orElseThrow();
        ReflectionTestUtils.setField(
            enrollment,
            "confirmedAt",
            LocalDateTime.now().minusDays(cancelAvailableDays + 1L)
        );

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(student.getId(), createResponse.enrollmentId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CANCEL_PERIOD_EXPIRED);
    }

    @Test
    @DisplayName("내 수강 신청 목록을 페이지네이션으로 조회할 수 있다.")
    void getMyEnrollments_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course1 = saveOpenCourse(creator, "Spring Boot 입문", 30);
        Course course2 = saveOpenCourse(creator, "JPA 기초", 30);

        CreateEnrollmentResponse response1 = enrollmentService.createEnrollment(student.getId(), course1.getId());
        enrollmentService.confirmEnrollment(student.getId(), response1.enrollmentId());
        enrollmentService.createEnrollment(student.getId(), course2.getId());

        PageResponse<GetMyEnrollmentListResponse> response =
            enrollmentService.getMyEnrollments(student.getId(), PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();

        assertThat(response.content())
            .extracting("courseTitle")
            .containsExactlyInAnyOrder("Spring Boot 입문", "JPA 기초");
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 결과가 없으면 빈 목록을 반환한다.")
    void getMyEnrollments_success_emptyList() {
        User student = saveUser("student1", UserRole.STUDENT);

        PageResponse<GetMyEnrollmentListResponse> response =
            enrollmentService.getMyEnrollments(student.getId(), PageRequest.of(0, 10));

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 내 수강 신청 목록을 조회할 수 없다.")
    void getMyEnrollments_fail_whenUserNotFound() {
        assertThatThrownBy(() -> enrollmentService.getMyEnrollments(999L, PageRequest.of(0, 10)))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("STUDENT가 아닌 사용자는 내 수강 신청 목록을 조회할 수 없다.")
    void getMyEnrollments_fail_whenUserIsNotStudent() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        assertThatThrownBy(() -> enrollmentService.getMyEnrollments(creator.getId(), PageRequest.of(0, 10)))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_LIST_GET_FORBIDDEN);
    }

    @Test
    @DisplayName("크리에이터는 본인이 개설한 강의의 확정 수강생 목록을 조회할 수 있다.")
    void getCourseStudents_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student1 = saveUser("student1", UserRole.STUDENT);
        User student2 = saveUser("student2", UserRole.STUDENT);
        User student3 = saveUser("student3", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        CreateEnrollmentResponse response1 = enrollmentService.createEnrollment(student1.getId(), course.getId());
        enrollmentService.confirmEnrollment(student1.getId(), response1.enrollmentId());

        CreateEnrollmentResponse response2 = enrollmentService.createEnrollment(student2.getId(), course.getId());
        enrollmentService.confirmEnrollment(student2.getId(), response2.enrollmentId());

        CreateEnrollmentResponse response3 = enrollmentService.createEnrollment(student3.getId(), course.getId());
        enrollmentService.cancelEnrollment(student3.getId(), response3.enrollmentId());

        List<GetCourseStudentListResponse> response =
            enrollmentService.getCourseStudents(creator.getId(), course.getId());

        assertThat(response).hasSize(2);
        assertThat(response)
            .extracting("studentName")
            .containsExactlyInAnyOrder("student1", "student2");
        assertThat(response)
            .extracting("confirmedAt")
            .doesNotContainNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 강의 수강생 목록을 조회할 수 없다.")
    void getCourseStudents_fail_whenUserNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.getCourseStudents(999L, course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("CREATOR가 아닌 사용자는 강의 수강생 목록을 조회할 수 없다.")
    void getCourseStudents_fail_whenUserIsNotCreator() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.getCourseStudents(student.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_STUDENT_LIST_GET_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 강의의 수강생 목록은 조회할 수 없다.")
    void getCourseStudents_fail_whenCourseNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        assertThatThrownBy(() -> enrollmentService.getCourseStudents(creator.getId(), 999L))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 개설한 강의가 아니면 수강생 목록을 조회할 수 없다.")
    void getCourseStudents_fail_whenNotCourseCreator() {
        User creator1 = saveUser("creator1", UserRole.CREATOR);
        User creator2 = saveUser("creator2", UserRole.CREATOR);
        Course course = saveOpenCourse(creator1, "Spring Boot 입문", 30);

        assertThatThrownBy(() -> enrollmentService.getCourseStudents(creator2.getId(), course.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_STUDENT_LIST_GET_FORBIDDEN);
    }

    @Test
    @DisplayName("확정 수강생이 없으면 빈 목록을 반환한다.")
    void getCourseStudents_success_emptyList() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveOpenCourse(creator, "Spring Boot 입문", 30);

        enrollmentService.createEnrollment(student.getId(), course.getId());

        List<GetCourseStudentListResponse> response =
            enrollmentService.getCourseStudents(creator.getId(), course.getId());

        assertThat(response).isEmpty();
    }

    private User saveUser(String name, UserRole role) {
        User user = User.builder()
                        .name(name)
                        .role(role)
                        .build();

        return userRepository.save(user);
    }

    private Course saveCourse(User creator, String title, int capacity) {
        Course course = Course.builder()
                            .creator(creator)
                            .title(title)
                            .description(title + " 설명입니다.")
                            .price(50000)
                            .capacity(capacity)
                            .startAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                            .endAt(LocalDateTime.of(2026, 8, 31, 23, 59))
                            .build();

        return courseRepository.save(course);
    }

    private Course saveOpenCourse(User creator, String title, int capacity) {
        Course course = saveCourse(creator, title, capacity);
        course.changeStatus(CourseStatus.OPEN);
        return courseRepository.save(course);
    }
}