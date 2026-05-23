package com.example.courseenrollment.course.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.dto.CreateCourseRequest;
import com.example.courseenrollment.course.dto.CreateCourseResponse;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
