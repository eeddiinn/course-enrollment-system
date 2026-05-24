package com.example.courseenrollment.course.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.dto.*;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateCourseResponse createCourse(Long userId, CreateCourseRequest request) {
        User creator = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (creator.getRole() != UserRole.CREATOR) {
            throw new CustomException(ErrorType.COURSE_CREATE_FORBIDDEN);
        }

        Course course = Course.builder()
                            .creator(creator)
                            .title(request.title())
                            .description(request.description())
                            .price(request.price())
                            .capacity(request.capacity())
                            .startAt(request.startAt())
                            .endAt(request.endAt())
                            .build();

        return new CreateCourseResponse(courseRepository.save(course).getId());
    }

    @Transactional
    public UpdateCourseStatusResponse updateCourseStatus(Long userId, Long courseId, UpdateCourseStatusRequest request) {
        User creator = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (creator.getRole() != UserRole.CREATOR) {
            throw new CustomException(ErrorType.COURSE_STATUS_CHANGE_FORBIDDEN);
        }

        Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new CustomException(ErrorType.COURSE_NOT_FOUND));

        // 해당 강의를 만든 크리에이터인지 확인
        if (!course.getCreator().getId().equals(userId)) {
            throw new CustomException(ErrorType.COURSE_STATUS_CHANGE_FORBIDDEN);
        }

        validateCourseStatusChange(course.getStatus(), request.status());

        course.changeStatus(request.status());

        return new UpdateCourseStatusResponse(course.getId(), course.getStatus());
    }

    // 현재 상태에서 요청한 상태로 바꿀 수 있는지
    private void validateCourseStatusChange(CourseStatus currentStatus, CourseStatus nextStatus) {
        if (currentStatus == CourseStatus.DRAFT && nextStatus == CourseStatus.OPEN) {
            return;
        }

        if (currentStatus == CourseStatus.OPEN && nextStatus == CourseStatus.CLOSED) {
            return;
        }

        throw new CustomException(ErrorType.INVALID_COURSE_STATUS);
    }

    @Transactional(readOnly = true)
    public List<GetCourseListResponse> getCourses(CourseStatus status) {
        List<Course> courses;

        if (status == null) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findAllByStatus(status);
        }

        return courses.stream()
                   .map(course -> new GetCourseListResponse(
                       course.getId(),
                       course.getTitle(),
                       course.getPrice(),
                       course.getStatus(),
                       course.getStartAt(),
                       course.getEndAt()
                   ))
                   .toList();
    }
}
