package com.example.courseenrollment.enrollment.repository;

import com.example.courseenrollment.enrollment.domain.Enrollment;
import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseIdAndStatusNot(Long studentId, Long courseId, EnrollmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.id = :enrollmentId")
    Optional<Enrollment> findByIdWithLock(Long enrollmentId);

    Page<Enrollment> findAllByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    List<Enrollment> findAllByCourseIdAndStatusOrderByConfirmedAtDesc(Long courseId, EnrollmentStatus status);
}