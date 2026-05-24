package com.example.courseenrollment.course.controller;

import com.example.courseenrollment.course.domain.CourseStatus;
import com.example.courseenrollment.course.dto.*;
import com.example.courseenrollment.course.service.CourseService;
import com.example.courseenrollment.global.response.ApiResponse;
import com.example.courseenrollment.global.response.SuccessType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateCourseResponse>> createCourse(@RequestHeader("userId") Long userId, @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessType.CREATE_COURSE_SUCCESS, courseService.createCourse(userId, request)));
    }

    @PatchMapping("/{courseId}/status")
    public ResponseEntity<ApiResponse<UpdateCourseStatusResponse>> updateCourseStatus(@RequestHeader("userId") Long userId, @PathVariable Long courseId, @Valid @RequestBody UpdateCourseStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessType.UPDATE_COURSE_STATUS_SUCCESS, courseService.updateCourseStatus(userId, courseId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GetCourseListResponse>>> getCourses(@RequestParam(required = false) CourseStatus status) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.GET_COURSE_LIST_SUCCESS, courseService.getCourses(status)));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<GetCourseDetailResponse>> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.GET_COURSE_DETAIL_SUCCESS, courseService.getCourse(courseId)));
    }
}
