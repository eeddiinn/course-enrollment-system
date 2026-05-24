package com.example.courseenrollment.enrollment.dto;

import java.time.LocalDateTime;

public record GetCourseStudentListResponse(
    Long enrollmentId,
    Long studentId,
    String studentName,
    LocalDateTime confirmedAt
) {
}
