package com.example.courseenrollment.course.dto;

import com.example.courseenrollment.course.domain.CourseStatus;

import java.time.LocalDateTime;

public record GetCourseDetailResponse(
    Long courseId,
    Long creatorId,
    String creatorName,
    String title,
    String description,
    int price,
    int capacity,
    int enrolledCount,
    CourseStatus status,
    LocalDateTime startAt,
    LocalDateTime endAt
) {
}
