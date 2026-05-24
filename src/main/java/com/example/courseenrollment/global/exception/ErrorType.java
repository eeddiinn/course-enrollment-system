package com.example.courseenrollment.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorType {

    /**
     * HTTP 400 (BAD REQUEST)
     */
    REQUEST_VALIDATION_EXCEPTION(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_COURSE_STATUS(HttpStatus.BAD_REQUEST, "올바르지 않은 강의 상태입니다."),

    /**
     * HTTP 403 (FORBIDDEN)
     */
    COURSE_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "강의 등록 권한이 없습니다."),
    COURSE_STATUS_CHANGE_FORBIDDEN(HttpStatus.FORBIDDEN, "강의 상태 변경 권한이 없습니다."),

    /**
     * HTTP 404 (NOT FOUND)
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강의입니다."),

    /**
     * HTTP 409 (CONFLICT)
     */

    /**
     * HTTP 500 (INTERNAL SERVER ERROR)
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 서버 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
