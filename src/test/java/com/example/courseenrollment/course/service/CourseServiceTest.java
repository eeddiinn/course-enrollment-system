package com.example.courseenrollment.course.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.dto.CreateCourseRequest;
import com.example.courseenrollment.course.dto.CreateCourseResponse;
import com.example.courseenrollment.course.dto.GetCourseDetailResponse;
import com.example.courseenrollment.course.dto.GetCourseListResponse;
import com.example.courseenrollment.course.dto.UpdateCourseStatusRequest;
import com.example.courseenrollment.course.dto.UpdateCourseStatusResponse;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("CREATOR는 강의를 등록할 수 있다.")
    void createCourse_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        CreateCourseRequest request = createCourseRequest("Spring Boot 입문");

        CreateCourseResponse response = courseService.createCourse(creator.getId(), request);

        Course savedCourse = courseRepository.findById(response.courseId()).orElseThrow();

        assertThat(response.courseId()).isNotNull();
        assertThat(savedCourse.getTitle()).isEqualTo("Spring Boot 입문");
        assertThat(savedCourse.getDescription()).isEqualTo("Spring Boot 입문 설명입니다.");
        assertThat(savedCourse.getPrice()).isEqualTo(50000);
        assertThat(savedCourse.getCapacity()).isEqualTo(30);
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(savedCourse.getEnrolledCount()).isEqualTo(0);
        assertThat(savedCourse.getCreator().getId()).isEqualTo(creator.getId());
    }

    @Test
    @DisplayName("CREATOR가 아닌 사용자는 강의를 등록할 수 없다.")
    void createCourse_fail_whenUserIsNotCreator() {
        User student = saveUser("student1", UserRole.STUDENT);

        CreateCourseRequest request = createCourseRequest("Spring Boot 입문");

        assertThatThrownBy(() -> courseService.createCourse(student.getId(), request))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_CREATE_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 강의를 등록할 수 없다.")
    void createCourse_fail_whenUserNotFound() {
        CreateCourseRequest request = createCourseRequest("Spring Boot 입문");

        assertThatThrownBy(() -> courseService.createCourse(999L, request))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상태를 DRAFT에서 OPEN으로 변경할 수 있다.")
    void updateCourseStatus_success_draftToOpen() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveCourse(creator, "Spring Boot 입문");

        UpdateCourseStatusResponse response = courseService.updateCourseStatus(
            creator.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        );

        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.status()).isEqualTo(CourseStatus.OPEN);
        assertThat(updatedCourse.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상태를 OPEN에서 CLOSED로 변경할 수 있다.")
    void updateCourseStatus_success_openToClosed() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveCourse(creator, "Spring Boot 입문");

        courseService.updateCourseStatus(
            creator.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        );

        UpdateCourseStatusResponse response = courseService.updateCourseStatus(
            creator.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.CLOSED)
        );

        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.status()).isEqualTo(CourseStatus.CLOSED);
        assertThat(updatedCourse.getStatus()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("강의 상태를 DRAFT에서 CLOSED로 바로 변경할 수 없다.")
    void updateCourseStatus_fail_draftToClosed() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveCourse(creator, "Spring Boot 입문");

        assertThatThrownBy(() -> courseService.updateCourseStatus(
            creator.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.CLOSED)
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_COURSE_STATUS);
    }

    @Test
    @DisplayName("CREATOR가 아닌 사용자는 강의 상태를 변경할 수 없다.")
    void updateCourseStatus_fail_whenUserIsNotCreator() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        User student = saveUser("student1", UserRole.STUDENT);
        Course course = saveCourse(creator, "Spring Boot 입문");

        assertThatThrownBy(() -> courseService.updateCourseStatus(
            student.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_STATUS_CHANGE_FORBIDDEN);
    }

    @Test
    @DisplayName("강의를 개설한 크리에이터가 아니면 강의 상태를 변경할 수 없다.")
    void updateCourseStatus_fail_whenUserIsNotCourseCreator() {
        User creator1 = saveUser("creator1", UserRole.CREATOR);
        User creator2 = saveUser("creator2", UserRole.CREATOR);
        Course course = saveCourse(creator1, "Spring Boot 입문");

        assertThatThrownBy(() -> courseService.updateCourseStatus(
            creator2.getId(),
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_STATUS_CHANGE_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 강의의 상태를 변경하면 실패한다.")
    void updateCourseStatus_fail_whenCourseNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        assertThatThrownBy(() -> courseService.updateCourseStatus(
            creator.getId(),
            999L,
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 강의 상태를 변경할 수 없다.")
    void updateCourseStatus_fail_whenUserNotFound() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveCourse(creator, "Spring Boot 입문");

        assertThatThrownBy(() -> courseService.updateCourseStatus(
            999L,
            course.getId(),
            new UpdateCourseStatusRequest(CourseStatus.OPEN)
        ))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상태 필터 없이 전체 강의 목록을 조회할 수 있다.")
    void getCourses_success_withoutStatusFilter() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        saveCourse(creator, "Spring Boot 입문");
        saveCourse(creator, "JPA 기초");
        saveCourse(creator, "Docker 배포 입문");

        List<GetCourseListResponse> response = courseService.getCourses(null);

        assertThat(response).hasSize(3);
        assertThat(response)
            .extracting("title")
            .containsExactlyInAnyOrder(
                "Spring Boot 입문",
                "JPA 기초",
                "Docker 배포 입문"
            );
    }

    @Test
    @DisplayName("OPEN 상태 강의 목록만 조회할 수 있다.")
    void getCourses_success_openStatusFilter() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        Course openCourse = saveCourse(creator, "Spring Boot 입문");
        saveCourse(creator, "JPA 기초");

        changeCourseStatus(openCourse, CourseStatus.OPEN);

        List<GetCourseListResponse> response = courseService.getCourses(CourseStatus.OPEN);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("Spring Boot 입문");
        assertThat(response.get(0).status()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("DRAFT 상태 강의 목록만 조회할 수 있다.")
    void getCourses_success_draftStatusFilter() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        saveCourse(creator, "Spring Boot 입문");
        Course openCourse = saveCourse(creator, "JPA 기초");

        changeCourseStatus(openCourse, CourseStatus.OPEN);

        List<GetCourseListResponse> response = courseService.getCourses(CourseStatus.DRAFT);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("Spring Boot 입문");
        assertThat(response.get(0).status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("CLOSED 상태 강의 목록만 조회할 수 있다.")
    void getCourses_success_closedStatusFilter() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        saveCourse(creator, "Spring Boot 입문");
        Course closedCourse = saveCourse(creator, "JPA 기초");

        changeCourseStatus(closedCourse, CourseStatus.OPEN);
        changeCourseStatus(closedCourse, CourseStatus.CLOSED);

        List<GetCourseListResponse> response = courseService.getCourses(CourseStatus.CLOSED);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("JPA 기초");
        assertThat(response.get(0).status()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("조건에 맞는 강의가 없으면 빈 목록을 반환한다.")
    void getCourses_success_emptyList() {
        User creator = saveUser("creator1", UserRole.CREATOR);

        saveCourse(creator, "Spring Boot 입문");
        saveCourse(creator, "JPA 기초");

        List<GetCourseListResponse> response = courseService.getCourses(CourseStatus.OPEN);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("강의 상세 정보를 조회할 수 있다.")
    void getCourse_success() {
        User creator = saveUser("creator1", UserRole.CREATOR);
        Course course = saveCourse(creator, "Spring Boot 입문");

        GetCourseDetailResponse response = courseService.getCourse(course.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.creatorId()).isEqualTo(creator.getId());
        assertThat(response.creatorName()).isEqualTo("creator1");
        assertThat(response.title()).isEqualTo("Spring Boot 입문");
        assertThat(response.description()).isEqualTo("Spring Boot 입문 설명입니다.");
        assertThat(response.price()).isEqualTo(50000);
        assertThat(response.capacity()).isEqualTo(30);
        assertThat(response.enrolledCount()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 8, 31, 23, 59));
    }

    @Test
    @DisplayName("존재하지 않는 강의를 상세 조회하면 실패한다.")
    void getCourse_fail_whenCourseNotFound() {
        assertThatThrownBy(() -> courseService.getCourse(999L))
            .isInstanceOf(CustomException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    private User saveUser(String name, UserRole role) {
        User user = User.builder()
                        .name(name)
                        .role(role)
                        .build();

        return userRepository.save(user);
    }

    private Course saveCourse(User creator, String title) {
        Course course = Course.builder()
                            .creator(creator)
                            .title(title)
                            .description(title + " 설명입니다.")
                            .price(50000)
                            .capacity(30)
                            .startAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                            .endAt(LocalDateTime.of(2026, 8, 31, 23, 59))
                            .build();

        return courseRepository.save(course);
    }

    private CreateCourseRequest createCourseRequest(String title) {
        return new CreateCourseRequest(
            title,
            title + " 설명입니다.",
            50000,
            30,
            LocalDateTime.of(2026, 6, 1, 9, 0),
            LocalDateTime.of(2026, 8, 31, 23, 59)
        );
    }

    private void changeCourseStatus(Course course, CourseStatus status) {
        course.changeStatus(status);
        courseRepository.save(course);
    }
}