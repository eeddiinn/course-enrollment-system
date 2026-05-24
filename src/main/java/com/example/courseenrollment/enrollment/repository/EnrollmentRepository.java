package com.example.courseenrollment.enrollment.repository;

import com.example.courseenrollment.enrollment.domain.Enrollment;
import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseIdAndStatusNot(Long studentId, Long courseId, EnrollmentStatus status
    );
}
