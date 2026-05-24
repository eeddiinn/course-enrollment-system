package com.example.courseenrollment.enrollment.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.enrollment.domain.Enrollment;
import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;
import com.example.courseenrollment.enrollment.dto.CreateEnrollmentResponse;
import com.example.courseenrollment.enrollment.repository.EnrollmentRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateEnrollmentResponse createEnrollment(Long userId, Long courseId) {
        User student = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (student.getRole() != UserRole.STUDENT) {
            throw new CustomException(ErrorType.ENROLLMENT_CREATE_FORBIDDEN);
        }

        Course course = courseRepository.findByIdWithLock(courseId)
                            .orElseThrow(() -> new CustomException(ErrorType.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new CustomException(ErrorType.COURSE_NOT_OPEN);
        }

        boolean alreadyEnrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(userId, courseId, EnrollmentStatus.CANCELLED);

        if (alreadyEnrolled) {
            throw new CustomException(ErrorType.ALREADY_ENROLLED);
        }

        if (course.getEnrolledCount() >= course.getCapacity()) {
            throw new CustomException(ErrorType.COURSE_CAPACITY_FULL);
        }

        Enrollment enrollment = Enrollment.builder()
                                    .student(student)
                                    .course(course)
                                    .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        return new CreateEnrollmentResponse(savedEnrollment.getId());
    }
}
