package com.example.courseenrollment.course.dto;

import com.example.courseenrollment.course.domain.CourseStatus;

import java.time.LocalDateTime;

public record GetCourseListResponse(
    Long courseId,
    String title,
    int price,
    CourseStatus status,
    LocalDateTime startAt,
    LocalDateTime endAt
) {
}
