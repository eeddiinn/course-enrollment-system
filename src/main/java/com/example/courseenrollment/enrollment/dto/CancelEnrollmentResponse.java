package com.example.courseenrollment.enrollment.dto;

import com.example.courseenrollment.enrollment.domain.EnrollmentStatus;

public record CancelEnrollmentResponse(
    Long enrollmentId,
    EnrollmentStatus status
) {
}