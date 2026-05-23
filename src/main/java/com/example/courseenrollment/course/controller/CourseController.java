package com.example.courseenrollment.course.controller;

import com.example.courseenrollment.course.dto.CreateCourseRequest;
import com.example.courseenrollment.course.dto.CreateCourseResponse;
import com.example.courseenrollment.course.service.CourseService;
import com.example.courseenrollment.global.response.ApiResponse;
import com.example.courseenrollment.global.response.SuccessType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateCourseResponse>> createCourse(@RequestHeader("userId") Long userId, @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessType.CREATE_COURSE_SUCCESS, courseService.createCourse(userId, request)));
    }
}
