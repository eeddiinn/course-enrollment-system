package com.example.courseenrollment.enrollment.dto;

import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;

import java.time.LocalDateTime;

public record GetMyEnrollmentListResponse(
    Long enrollmentId,
    Long courseId,
    String courseTitle,
    int price,
    EnrollmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt
) {
}
