package com.example.courseenrollment.enrollment.service;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.repository.CourseRepository;
import com.example.courseenrollment.enrollment.domain.Enrollment;
import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;
import com.example.courseenrollment.enrollment.dto.*;
import com.example.courseenrollment.enrollment.repository.EnrollmentRepository;
import com.example.courseenrollment.global.exception.CustomException;
import com.example.courseenrollment.global.exception.ErrorType;
import com.example.courseenrollment.global.response.PageResponse;
import com.example.courseenrollment.user.domain.User;
import com.example.courseenrollment.user.domain.UserRole;
import com.example.courseenrollment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    @Value("${app.enrollment.cancel-available-days}")
    private int cancelAvailableDays;

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

        course.increaseEnrolledCount();

        return new CreateEnrollmentResponse(savedEnrollment.getId());
    }

    @Transactional
    public ConfirmEnrollmentResponse confirmEnrollment(Long userId, Long enrollmentId) {
        User student = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (student.getRole() != UserRole.STUDENT) {
            throw new CustomException(ErrorType.ENROLLMENT_CONFIRM_FORBIDDEN);
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                                    .orElseThrow(() -> new CustomException(ErrorType.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getStudent().getId().equals(userId)) {
            throw new CustomException(ErrorType.ENROLLMENT_CONFIRM_FORBIDDEN);
        }

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new CustomException(ErrorType.ENROLLMENT_CONFIRM_NOT_ALLOWED);
        }

        enrollment.confirm(LocalDateTime.now());

        return new ConfirmEnrollmentResponse(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional
    public CancelEnrollmentResponse cancelEnrollment(Long userId, Long enrollmentId) {
        User student = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (student.getRole() != UserRole.STUDENT) {
            throw new CustomException(ErrorType.ENROLLMENT_CANCEL_FORBIDDEN);
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                                    .orElseThrow(() -> new CustomException(ErrorType.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getStudent().getId().equals(userId)) {
            throw new CustomException(ErrorType.ENROLLMENT_CANCEL_FORBIDDEN);
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new CustomException(ErrorType.ENROLLMENT_ALREADY_CANCELLED);
        }

        LocalDateTime now = LocalDateTime.now();

        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            validateCancelPeriod(enrollment, now);
        }

        Course course = courseRepository.findByIdWithLock(enrollment.getCourse().getId())
                            .orElseThrow(() -> new CustomException(ErrorType.COURSE_NOT_FOUND));

        enrollment.cancel(now);

        course.decreaseEnrolledCount();

        return new CancelEnrollmentResponse(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional(readOnly = true)
    public PageResponse<GetMyEnrollmentListResponse> getMyEnrollments(Long userId, Pageable pageable) {
        User student = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (student.getRole() != UserRole.STUDENT) {
            throw new CustomException(ErrorType.ENROLLMENT_LIST_GET_FORBIDDEN);
        }

        Page<GetMyEnrollmentListResponse> enrollments =
            enrollmentRepository.findAllByStudentIdOrderByCreatedAtDesc(userId, pageable)
                .map(enrollment -> new GetMyEnrollmentListResponse(
                    enrollment.getId(),
                    enrollment.getCourse().getId(),
                    enrollment.getCourse().getTitle(),
                    enrollment.getCourse().getPrice(),
                    enrollment.getStatus(),
                    enrollment.getCreatedAt(),
                    enrollment.getConfirmedAt(),
                    enrollment.getCancelledAt()
                ));

        return PageResponse.from(enrollments);
    }

    @Transactional(readOnly = true)
    public List<GetCourseStudentListResponse> getCourseStudents(Long userId, Long courseId) {
        User creator = userRepository.findById(userId)
                           .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        if (creator.getRole() != UserRole.CREATOR) {
            throw new CustomException(ErrorType.COURSE_STUDENT_LIST_GET_FORBIDDEN);
        }

        Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new CustomException(ErrorType.COURSE_NOT_FOUND));

        if (!course.getCreator().getId().equals(userId)) {
            throw new CustomException(ErrorType.COURSE_STUDENT_LIST_GET_FORBIDDEN);
        }

        List<Enrollment> enrollments = enrollmentRepository.findAllByCourseIdAndStatusOrderByConfirmedAtDesc(courseId, EnrollmentStatus.CONFIRMED);

        return enrollments.stream()
                   .map(enrollment -> new GetCourseStudentListResponse(
                       enrollment.getId(),
                       enrollment.getStudent().getId(),
                       enrollment.getStudent().getName(),
                       enrollment.getConfirmedAt()
                   ))
                   .toList();
    }

    // 취소 가능 기간인지 검증
    private void validateCancelPeriod(Enrollment enrollment, LocalDateTime now) {
        LocalDateTime cancelDeadline = enrollment.getConfirmedAt().plusDays(cancelAvailableDays);

        if (now.isAfter(cancelDeadline)) {
            throw new CustomException(ErrorType.ENROLLMENT_CANCEL_PERIOD_EXPIRED);
        }
    }
}
