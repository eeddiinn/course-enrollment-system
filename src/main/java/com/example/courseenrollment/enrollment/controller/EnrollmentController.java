package com.example.courseenrollment.enrollment.controller;

import com.example.courseenrollment.enrollment.dto.*;
import com.example.courseenrollment.enrollment.service.EnrollmentService;
import com.example.courseenrollment.global.response.ApiResponse;
import com.example.courseenrollment.global.response.PageResponse;
import com.example.courseenrollment.global.response.SuccessType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<ApiResponse<CreateEnrollmentResponse>> createEnrollment(@RequestHeader("userId") Long userId, @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessType.CREATE_ENROLLMENT_SUCCESS, enrollmentService.createEnrollment(userId, courseId)));
    }

    @PatchMapping("/enrollments/{enrollmentId}/confirm")
    public ResponseEntity<ApiResponse<ConfirmEnrollmentResponse>> confirmEnrollment(@RequestHeader("userId") Long userId, @PathVariable Long enrollmentId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.ENROLLMENT_CONFIRM_SUCCESS,enrollmentService.confirmEnrollment(userId, enrollmentId)));
    }

    @PatchMapping("/enrollments/{enrollmentId}/cancel")
    public ResponseEntity<ApiResponse<CancelEnrollmentResponse>> cancelEnrollment(@RequestHeader("userId") Long userId, @PathVariable Long enrollmentId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.ENROLLMENT_CANCEL_SUCCESS,enrollmentService.cancelEnrollment(userId, enrollmentId)));
    }

    @GetMapping("/users/me/enrollments")
    public ResponseEntity<ApiResponse<PageResponse<GetMyEnrollmentListResponse>>> getMyEnrollments(@RequestHeader("userId") Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.GET_ENROLLMENT_LIST_SUCCESS, enrollmentService.getMyEnrollments(userId, PageRequest.of(page, size))));
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<ApiResponse<List<GetCourseStudentListResponse>>> getCourseStudents(@RequestHeader("userId") Long userId, @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(SuccessType.GET_COURSE_STUDENT_LIST_SUCCESS,enrollmentService.getCourseStudents(userId, courseId)));
    }
}