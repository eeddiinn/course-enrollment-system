package com.example.courseenrollment.enrollment.domain;

import com.example.courseenrollment.course.domain.Course;
import com.example.courseenrollment.global.entity.BaseTimeEntity;
import com.example.courseenrollment.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "enrollments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseTimeEntity {

    private static final int CANCELLATION_AVAILABLE_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public Enrollment(User student, Course course) {
        this.student = student;
        this.course = course;
        this.status = EnrollmentStatus.PENDING;
    }
}