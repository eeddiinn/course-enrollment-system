package com.example.courseenrollment.enrollment.controller;

import com.example.courseenrollment.enrollment.dto.CreateEnrollmentResponse;
import com.example.courseenrollment.enrollment.service.EnrollmentService;
import com.example.courseenrollment.global.response.ApiResponse;
import com.example.courseenrollment.global.response.SuccessType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<ApiResponse<CreateEnrollmentResponse>> createEnrollment(@RequestHeader("userId") Long userId, @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessType.CREATE_ENROLLMENT_SUCCESS, enrollmentService.createEnrollment(userId, courseId)));
    }
}
