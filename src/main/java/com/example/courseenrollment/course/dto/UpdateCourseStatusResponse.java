package com.example.courseenrollment.course.dto;

import com.example.courseenrollment.course.domain.CourseStatus;

public record UpdateCourseStatusResponse(
    Long courseId,
    CourseStatus status
) {
}
