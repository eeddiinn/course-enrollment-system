package com.example.courseenrollment.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record CreateCourseRequest(
    @NotBlank(message = "강의 제목은 필수입니다.")
    String title,

    @NotBlank(message = "강의 설명은 필수입니다.")
    String description,

    @PositiveOrZero(message = "강의 가격은 0원 이상이어야 합니다.")
    int price,

    @Min(value = 1, message = "최대 수강 인원은 1명 이상이어야 합니다.")
    int capacity,

    @NotNull(message = "수강 시작 일시는 필수입니다.")
    LocalDateTime startAt,

    @NotNull(message = "수강 종료 일시는 필수입니다.")
    LocalDateTime endAt
) {
}
