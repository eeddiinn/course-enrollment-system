package com.example.courseenrollment.global.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum SuccessType {

    /**
     * HTTP 200 (OK)
     */
    PROCESS_SUCCESS(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    UPDATE_COURSE_STATUS_SUCCESS(HttpStatus.OK, "강의 상태가 변경되었습니다."),

    /**
     * HTTP 201 (CREATED)
     */
    CREATE_COURSE_SUCCESS(HttpStatus.CREATED, "강의가 등록되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
