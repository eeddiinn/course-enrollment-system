package com.example.courseenrollment.course.dto;

import com.example.courseenrollment.course.domain.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCourseStatusRequest(
    @NotNull(message = "강의 상태는 필수입니다.")
    CourseStatus status
) {
}