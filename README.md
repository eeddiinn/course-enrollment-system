# 수강 신청 시스템 

> BE-A 과제: 수강 신청 시스템  
> 유형: CRUD + 비즈니스 규칙형


## 프로젝트 개요
### 시나리오
- 크리에이터(강사)는 강의를 개설하고 수강 정원, 가격, 기간을 설정
- 클래스메이트(수강생)는 원하는 강의에 수강 신청
- 정원이 초과되면 신청이 불가
- 신청 후 결제가 완료되어야 수강 확정
- 수강 확정 후 일정 기간 내에는 취소가 가능하며, 이후에는 불가
  
### 핵심 기능

| 구분 | 기능 |
| --- | --- |
| 강의 관리 | 강의 등록, 강의 상태 변경, 강의 목록 조회, 강의 상세 조회 |
| 수강 신청 관리 | 수강 신청, 결제 확정, 수강 취소, 내 수강 신청 목록 조회 |
| 정원 관리 | 정원 초과 신청 방지, 마지막 자리 동시 신청 제어 |
| 선택 구현 | 수강 취소 가능 기간 제한, 신청 내역 페이지네이션, 강의별 수강생 목록 조회 |

### 구현 API

| No | 기능 | Method | URL |
| --- | --- | --- | --- |
| 1 | 강의 등록 | POST | `/api/courses` |
| 2 | 강의 상태 변경 | PATCH | `/api/courses/{courseId}/status` |
| 3 | 강의 목록 조회 | GET | `/api/courses` |
| 4 | 강의 상세 조회 | GET | `/api/courses/{courseId}` |
| 5 | 수강 신청 | POST | `/api/courses/{courseId}/enrollments` |
| 6 | 결제 확정 처리 | PATCH | `/api/enrollments/{enrollmentId}/confirm` |
| 7 | 수강 취소 | PATCH | `/api/enrollments/{enrollmentId}/cancel` |
| 8 | 내 수강 신청 목록 조회 | GET | `/api/users/me/enrollments` |
| 9 | 강의별 수강생 목록 조회 | GET | `/api/courses/{courseId}/students` |

---

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot |
| ORM | Spring Data JPA |
| Database | MySQL |
| Test Database | H2 |
| Build Tool | Gradle |
| Test | JUnit 5, AssertJ |

---

## 실행 방법

본 프로젝트는 **로컬 실행**을 기준으로 작성했습니다.  

### 1. MySQL 데이터베이스 생성

```sql
CREATE DATABASE course_enrollment;
```

### 2. 환경 변수 설정

로컬 실행 전 DB 접속 정보를 환경 변수로 설정합니다.

```env
DB_URL=jdbc:mysql://localhost:3306/course_enrollment?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=username
DB_PASSWORD=password
```
`.env` 파일과 실제 DB 계정 정보는 Git에 포함하지 않습니다.

### 3. 애플리케이션 실행

IntelliJ IDEA에서 `CourseEnrollmentSystemApplication`의 `main` 메서드를 실행합니다.

### 4. 실행 확인

서버 실행 후 아래 주소로 API를 호출할 수 있습니다.
```http
http://localhost:8080
```
API 요청 및 동작 확인은 Postman을 사용했습니다.

---

## 요구사항 해석 및 가정

### 1. 인증/인가 처리

- 과제 조건에서 인증/인가는 간략히 처리해도 된다고 되어 있어 별도의 로그인, JWT, Spring Security 인증은 구현하지 않았습니다.

- 대신 요청 Header의 `userId` 값을 기준으로 사용자를 식별합니다.

```http
userId: 1
```

- 사용자는 역할에 따라 접근 가능한 기능이 다릅니다.
- 강의 목록 조회, 강의 상세 조회는 누구나 접근할 수 있다고 가정하였습니다.

| 역할 | 가능 기능 |
| --- | --- |
| `CREATOR` | 강의 등록, 강의 상태 변경, 본인 강의의 수강생 목록 조회 |
| `STUDENT` | 수강 신청, 결제 확정, 수강 취소, 내 수강 신청 목록 조회 |

---

### 2. 강의 상태 흐름

- 강의 상태는 다음 흐름만 허용했습니다.

```text
DRAFT → OPEN → CLOSED
```

| 상태 | 의미 | 수강 신청 가능 여부 |
| --- | --- | --- |
| `DRAFT` | 초안 | 불가능 |
| `OPEN` | 모집 중 | 가능 |
| `CLOSED` | 모집 마감 | 불가능 |

- 허용되지 않는 상태 변경이나 존재하지 않는 상태 값은 `400 Bad Request`로 처리했습니다.

---

### 3. 수강 신청 상태 흐름

- 수강 신청 상태는 다음 흐름으로 관리했습니다.

```text
PENDING → CONFIRMED → CANCELLED
```

| 상태 | 의미 |
| --- | --- |
| `PENDING` | 신청 완료, 결제 대기 |
| `CONFIRMED` | 결제 완료, 수강 확정 |
| `CANCELLED` | 취소됨 |

- 외부 결제 시스템은 구현하지 않고, 결제 확정 API 호출을 통해 `PENDING` 상태를 `CONFIRMED` 상태로 변경했습니다.

---

### 4. 정원 차감 시점에 대한 고민

- 요구사항에는 다음 두 조건이 함께 존재했습니다.

```text
정원이 초과되면 신청이 불가합니다.
신청 후 결제가 완료되어야 수강 확정됩니다.
```

- 이 부분에서 가장 고민한 부분은 **정원을 수강 신청 시점에 차감할지, 신청 후 결제 확정 시점에 차감할지**였습니다.

최종적으로는 다음과 같이 해석했습니다.

| 항목 | 결정 |
| --- | --- |
| 좌석 선점 시점 | 수강 신청 성공 시점 |
| PENDING 상태 | 정원을 차지함 |
| 수강 신청 완료 시 | `courses.enrolledCount` 1 증가 |
| 결제 완료 시 | `enrolledCount` 증가 없음 |
| 수강 취소 시 | `courses.enrolledCount` 1 감소 |

- 이렇게 설계한 이유는 **정원 초과 시 신청 불가** 요구사항과 **마지막 자리에 여러 명이 동시에 신청하는 상황**을 수강 신청 단계에서 제어하기 위해서입니다.

  - 따라서 결제하기 전 단계인 `PENDING` 상태일때 좌석을 점유하는 신청으로 가정했습니다.

---

### 5. 중복 신청 정책

- 동일 사용자가 같은 강의에 여러 번 신청하는 것을 방지하기 위해 중복 신청을 검사했습니다.

| 기존 신청 상태 | 재신청 가능 여부 |
| --- | --- |
| `PENDING` | 불가능 |
| `CONFIRMED` | 불가능 |
| `CANCELLED` | 가능 |

- `CANCELLED` 상태는 이미 취소된 신청이므로 다시 신청할 수 있도록 허용했습니다.

---

### 6. 동시성 제어 방식

- 동시에 여러 명이 마지막 자리에 신청하는 상황을 고려해 수강 신청 시 강의 데이터를 비관적 락으로 조회했습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Course c where c.id = :courseId")
Optional<Course> findByIdWithLock(Long courseId);
```

- 이를 통해 같은 강의의 `enrolledCount`를 동시에 수정하지 못하게 막고, 정원을 초과하는 신청이 생성되지 않도록 했습니다.

동시성 테스트에서는 다음 상황을 검증했습니다.

| 테스트 상황 | 기대 결과 |
| --- | --- |
| 정원 10명, 기존 신청 9명, 20명 동시 신청 | 1명만 성공 |
| 정원 10명, 기존 신청 5명, 20명 동시 신청 | 5명만 성공 |

- 추가로 실패한 요청이 모두 `COURSE_CAPACITY_FULL` 예외인지 확인하고, 예상하지 못한 예외가 발생하지 않는지도 검증했습니다.

---

### 7. 수강 취소 가능 기간

- 선택 구현 사항인 **수강 취소 가능 기간 제한**을 구현했습니다.

- `CONFIRMED` 상태의 수강 신청은 결제 확정 후 설정된 기간 이내에만 취소할 수 있습니다.

- 취소 가능 기간은 코드에 고정하지 않고 `application.yml`에서 관리했습니다.

```yaml
app:
  enrollment:
    cancel-available-days: 7
```

- 기본값은 예시로 7일로 설정했습니다.

---

### 8. 목록 조회 페이지네이션

- 선택 구현 사항인 **신청 내역 페이지네이션**을 구현했습니다.

```http
GET /api/users/me/enrollments?page=0&size=10
```

- Spring Data JPA의 `Pageable`을 사용하여 신청 내역을 페이지 단위로 조회합니다.

---

### 9. 강의별 수강생 목록의 의미

- 선택 구현 사항의 “강의별 수강생 목록 조회”는 단순 신청자 전체가 아니라, **결제 확정이 완료된 실제 수강생 목록**으로 해석했습니다.

- 따라서 해당 API에서는 `CONFIRMED` 상태의 수강 신청만 조회합니다.

| 상태 | 강의별 수강생 목록 포함 여부 |
| --- | --- |
| `PENDING` | 제외 |
| `CONFIRMED` | 포함 |
| `CANCELLED` | 제외 |

---

## 설계 결정과 이유

### 1. 도메인별 패키지 분리

- 강의와 수강 신청은 서로 연결되어 있지만 담당하는 책임이 다르다고 판단하여 `course`, `enrollment`, `user` 도메인으로 패키지를 분리했습니다.

| 도메인 | 주요 책임 |
| --- | --- |
| `course` | 강의 등록, 상태 변경, 목록/상세 조회, 정원 관리 |
| `enrollment` | 수강 신청, 결제 확정, 수강 취소, 신청 목록 조회 |
| `user` | 사용자 정보와 역할 관리 |

---

### 2. Entity 중심의 상태 변경

- 상태 변경은 Service에서 필드를 직접 수정하지 않고 Entity 메서드를 통해 처리했습니다.

```java
course.changeStatus(nextStatus);
course.increaseEnrolledCount();
course.decreaseEnrolledCount();

enrollment.confirm(now);
enrollment.cancel(now);
```

- 상태 변경 책임을 도메인 객체 내부에 두어 비즈니스 의미가 코드에 더 명확하게 드러나도록 하기 위해서입니다.

---

### 3. 공통 응답 포맷

- 모든 API는 공통 응답 형식을 사용합니다.

```json
{
  "code": 200,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

- 성공 응답은 SuccessType, 실패 응답은 ErrorType을 기준으로 관리했습니다.
- API마다 응답 형식이 달라지는 것을 방지하고, 클라이언트가 일관된 구조로 응답을 처리할 수 있도록 하기 위해서입니다.

---

### 4. Validation과 Business Exception 분리

- 요청 값 자체의 유효성 검증과 비즈니스 규칙 위반을 분리했습니다.

| 구분 | 처리 방식 |
| --- | --- |
| 요청 값 검증 | Bean Validation |
| 비즈니스 규칙 위반 | `CustomException` + `ErrorType` |

예시:

| 상황 | 처리 |
| --- | --- |
| 제목 누락, 가격 음수, 정원 1 미만 | Validation Error |
| 권한 없음, 존재하지 않는 강의, 정원 마감, 잘못된 상태 변경 | CustomException |

---

## 미구현 / 제약사항

| 항목 | 내용 |
| --- | --- |
| 인증/인가 | 실제 로그인, JWT, Spring Security는 구현하지 않았습니다. Header의 `userId`로 대체했습니다. |
| 결제 시스템 | 외부 결제 시스템은 연동하지 않았습니다. 결제 확정 API 호출로 결제 완료를 대체했습니다. |
| 대기열 | waitlist 기능은 구현하지 않았습니다. 정원 마감 시 대기 등록 없이 신청 실패로 처리했습니다. |
| PENDING 만료 | 결제 대기 상태가 오래 유지되어도 자동 취소되지는 않습니다. |
| Docker | 별도 Docker 실행 환경은 구성하지 않았습니다. 로컬 MySQL 실행을 기준으로 작성했습니다. |

---

## AI 활용 범위

- 본 과제 수행 과정에서 AI 도구를 보조적으로 활용했습니다.
- 특히 처음 접하거나 익숙하지 않은 구현 방식은 개념을 확인한 뒤, 프로젝트 요구사항에 맞게 직접 적용하고 테스트를 통해 검증했습니다.


| 활용 범위 | 내용 |
| --- | --- |
| 요구사항 해석 검토 | 수강 신청 상태, 정원 차감 시점, 수강생 목록 의미 다시 한번 검토 |
| 서비스 로직 및 예외 케이스 점검 | 수강 신청, 결제 확정, 수강 취소 흐름에서 상태 변경 기준과 권한 없음, 중복 신청, 정원 마감 등 실패 케이스 누락 여부 확인 |
| 구현 방식 학습 및 검토 | 페이지네이션, 비관적 락, 동시성 테스트 등 구현에 필요한 개념과 적용 방식 확인 |
| API 명세 및 샘플 데이터 정리 | 요청/응답 예시, Postman 테스트용 강의 제목·설명·가격·정원 등의 예시 데이터 구성 |
| 테스트 케이스 도출 | 테스트 코드 작성 경험이 부족한 부분에 대해 서비스 테스트와 동시성 테스트의 구조 및 검증 방식 참고 |

- 최종 코드 구조, 비즈니스 규칙 적용 방식, 예외 처리 기준, 테스트 통과 여부는 직접 검토하여 반영했습니다.

---

## API 목록 및 예시

### 1. 강의 등록

```http
POST /api/courses
```

- 크리에이터가 새로운 강의를 등록합니다. 등록된 강의의 초기 상태는 `DRAFT`입니다.

#### Request Header

```http
Content-Type: application/json
userId: 1
```

#### Request Body

```json
{
  "title": "Spring Boot 입문",
  "description": "Spring Boot와 JPA를 활용한 백엔드 입문 강의입니다.",
  "price": 50000,
  "capacity": 30,
  "startAt": "2026-06-01T09:00:00",
  "endAt": "2026-08-31T23:59:00"
}
```

#### Success Response

```json
{
  "code": 201,
  "message": "강의가 등록되었습니다.",
  "data": {
    "courseId": 1
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 400 | 요청 값이 유효하지 않은 경우 | 요청 값이 올바르지 않습니다. |
| 403 | CREATOR가 아닌 사용자가 요청한 경우 | 강의 등록 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |

---

### 2. 강의 상태 변경

```http
PATCH /api/courses/{courseId}/status
```

- 크리에이터가 본인이 개설한 강의의 상태를 변경합니다.

#### Request Header

```http
Content-Type: application/json
userId: 1
```
#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| courseId | Long | 필수 | 상태를 변경할 강의 ID |

#### Request Body

```json
{
  "status": "OPEN"
}
```

#### Success Response

```json
{
  "code": 200,
  "message": "강의 상태가 변경되었습니다.",
  "data": {
    "courseId": 1,
    "status": "OPEN"
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 400 | 존재하지 않는 상태 값 또는 허용되지 않는 상태 변경 | 올바르지 않은 강의 상태입니다. |
| 403 | 해당 강의를 개설한 크리에이터가 아닌 경우 | 강의 상태 변경 권한이 없습니다. |
| 404 | 존재하지 않는 강의 | 존재하지 않는 강의입니다. |

---

### 3. 강의 목록 조회

```http
GET /api/courses?status=OPEN
```

- 상태 필터를 전달하지 않으면 전체 강의를 조회합니다.

#### Query Parameter

| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| status | String | 선택 | 조회할 강의 상태 |

#### Success Response

```json
{
    "code": 200,
    "message": "강의 목록 조회에 성공했습니다.",
    "data": [
        {
            "courseId": 1,
            "title": "Spring Boot 입문",
            "price": 50000,
            "status": "DRAFT",
            "startAt": "2026-06-01T09:00:00",
            "endAt": "2026-08-31T23:59:00"
        },
        {
            "courseId": 2,
            "title": "JPA 기초",
            "price": 40000,
            "status": "OPEN",
            "startAt": "2026-07-01T10:00:00",
            "endAt": "2026-08-15T18:00:00"
        },
        {
            "courseId": 3,
            "title": "React 실전 프로젝트",
            "price": 60000,
            "status": "CLOSED",
            "startAt": "2026-06-15T09:00:00",
            "endAt": "2026-09-15T18:00:00"
        },
        {
            "courseId": 4,
            "title": "Docker 배포 입문",
            "price": 30000,
            "status": "CLOSED",
            "startAt": "2026-08-01T13:00:00",
            "endAt": "2026-08-31T18:00:00"
        }
    ]
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 400 | 존재하지 않는 상태 값으로 필터링 | 올바르지 않은 강의 상태입니다. |

---

### 4. 강의 상세 조회

```http
GET /api/courses/{courseId}
```

- 특정 강의의 상세 정보를 조회합니다.

#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| courseId | Long | 필수 | 조회 강의 ID |

#### Success Response

```json
{
  "code": 200,
  "message": "강의 상세 조회에 성공했습니다.",
  "data": {
    "courseId": 1,
    "creatorId": 1,
    "creatorName": "creator1",
    "title": "Spring Boot 입문",
    "description": "Spring Boot와 JPA를 활용한 백엔드 입문 강의입니다.",
    "price": 50000,
    "capacity": 30,
    "enrolledCount": 0,
    "status": "OPEN",
    "startAt": "2026-06-01T09:00:00",
    "endAt": "2026-08-31T23:59:00"
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 404 | 존재하지 않는 강의 | 존재하지 않는 강의입니다. |

---

### 5. 수강 신청

```http
POST /api/courses/{courseId}/enrollments
```

수강생이 특정 강의에 수강 신청합니다.

- `OPEN` 상태의 강의만 신청할 수 있습니다.
- 신청 성공 시 상태는 `PENDING`입니다.
- `PENDING` 상태의 신청도 좌석을 점유합니다.
- 신청 성공 시 `courses.enrolledCount`가 1 증가합니다.

#### Request Header

```http
Content-Type: application/json
userId: 2
```

#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| courseId | Long | 필수 | 수강 신청할 강의 ID |


#### Request Body

```json
{}
```

#### Success Response

```json
{
  "code": 201,
  "message": "수강 신청이 완료되었습니다.",
  "data": {
    "enrollmentId": 1
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 400 | OPEN 상태가 아닌 강의 | 수강 신청 가능한 강의가 아닙니다. |
| 403 | STUDENT가 아닌 사용자가 요청 | 수강 신청 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 존재하지 않는 강의 | 존재하지 않는 강의입니다. |
| 409 | 이미 신청한 강의 | 이미 신청한 강의입니다. |
| 409 | 수강 정원 마감 | 수강 정원이 마감되었습니다. |

---

### 6. 결제 확정 처리

```http
PATCH /api/enrollments/{enrollmentId}/confirm
```

- 수강 신청의 결제 확정 처리를 수행합니다.

- 외부 결제 시스템은 구현하지 않고, 수강생 본인이 결제를 완료했다고 가정하여 `PENDING` 상태의 신청을 `CONFIRMED` 상태로 변경합니다.

- 수강 신청 시점에 이미 좌석을 점유했기 때문에 결제 확정 시 `enrolledCount`는 증가하지 않습니다.

#### Request Header

```http
Content-Type: application/json
userId: 2
```

#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| enrollmentId | Long | 필수 | 결제 확정 처리할 수강 신청 ID |


#### Request Body

```json
{}
```

#### Success Response

```json
{
  "code": 200,
  "message": "결제가 확정되었습니다.",
  "data": {
    "enrollmentId": 1,
    "status": "CONFIRMED"
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 403 | STUDENT가 아닌 사용자 또는 본인 신청이 아닌 경우 | 결제 확정 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 존재하지 않는 수강 신청 | 존재하지 않는 수강 신청입니다. |
| 409 | PENDING 상태가 아닌 신청 | 결제 확정할 수 없는 수강 신청 상태입니다. |

---

### 7. 수강 취소

```http
PATCH /api/enrollments/{enrollmentId}/cancel
```

- 수강 신청을 취소합니다.
- `PENDING` 또는 `CONFIRMED` 상태의 신청을 `CANCELLED` 상태로 변경합니다.
- `CONFIRMED` 상태의 경우 결제 확정 후 설정 값 기간 이내에만 취소할 수 있습니다.
- 취소 성공 시 `cancelledAt`이 저장됩니다.
- 취소 성공 시 `courses.enrolledCount`가 1 감소합니다.

#### Request Header

```http
Content-Type: application/json
userId: 2
```

#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| enrollmentId | Long | 필수 | 취소할 수강 신청 ID |


#### Request Body

```json
{}
```

#### Success Response

```json
{
  "code": 200,
  "message": "수강 신청이 취소되었습니다.",
  "data": {
    "enrollmentId": 1,
    "status": "CANCELLED"
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 400 | 이미 취소된 수강 신청 | 이미 취소된 수강 신청입니다. |
| 400 | 취소 가능 기간이 지난 경우 | 취소 가능 기간이 지났습니다. |
| 403 | STUDENT가 아닌 사용자 또는 본인 신청이 아닌 경우 | 수강 신청 취소 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 존재하지 않는 수강 신청 | 존재하지 않는 수강 신청입니다. |
| 404 | 연결된 강의가 존재하지 않는 경우 | 존재하지 않는 강의입니다. |

---

### 8. 내 수강 신청 목록 조회

```http
GET /api/users/me/enrollments?page=0&size=10
```

- 현재 사용자의 수강 신청 내역을 조회합니다.
- `PENDING`, `CONFIRMED`, `CANCELLED` 상태의 신청 내역을 모두 조회합니다.
- 페이지네이션을 통해 일정 개수씩 나누어 조회합니다.

#### Request Header

```http
Content-Type: application/json
userId: 2
```

#### Query Parameter

| 이름 | 타입 | 필수 여부 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| page | int | 선택 | 0 | 조회할 페이지 번호 |
| size | int | 선택 | 10 | 한 페이지에 조회할 신청 내역 개수 |

#### Success Response

```json
{
  "code": 200,
  "message": "내 수강 신청 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "enrollmentId": 1,
        "courseId": 1,
        "courseTitle": "Spring Boot 입문",
        "price": 50000,
        "status": "CONFIRMED",
        "createdAt": "2026-05-22T21:20:00",
        "confirmedAt": "2026-05-22T21:30:00",
        "cancelledAt": null
      },
      {
        "enrollmentId": 2,
        "courseId": 2,
        "courseTitle": "JPA 기초",
        "price": 40000,
        "status": "PENDING",
        "createdAt": "2026-05-23T10:00:00",
        "confirmedAt": null,
        "cancelledAt": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "last": true
  }
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 403 | STUDENT가 아닌 사용자가 요청 | 수강 신청 목록 조회 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |

---

### 9. 강의별 수강생 목록 조회

```http
GET /api/courses/{courseId}/students
```

- 크리에이터가 본인이 개설한 강의의 확정 수강생 목록을 조회합니다.
- 해당 강의를 개설한 크리에이터만 조회할 수 있습니다.
- `CONFIRMED` 상태의 수강 신청만 수강생으로 간주합니다.
- `PENDING`, `CANCELLED` 상태의 신청자는 조회 대상에서 제외합니다.

#### Request Header

```http
Content-Type: application/json
userId: 1
```

#### Path Variable
| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| courseId | Long | 필수 | 수강생 목록을 조회할 강의 ID |


#### Success Response

```json
{
  "code": 200,
  "message": "강의 수강생 목록 조회에 성공했습니다.",
  "data": [
    {
      "enrollmentId": 1,
      "studentId": 2,
      "studentName": "student1",
      "confirmedAt": "2026-05-22T21:30:00"
    },
    {
      "enrollmentId": 2,
      "studentId": 3,
      "studentName": "student2",
      "confirmedAt": "2026-05-23T10:30:00"
    }
  ]
}
```

#### 주요 실패 케이스

| HTTP Status | 사유 | Message |
| --- | --- | --- |
| 403 | CREATOR가 아닌 사용자 또는 본인 강의가 아닌 경우 | 강의 수강생 목록 조회 권한이 없습니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 존재하지 않는 강의 | 존재하지 않는 강의입니다. |

---

## 데이터 모델 설명

- `users`, `courses`, `enrollments` 세 개의 주요 테이블로 구성됩니다.

<img width="1391" height="486" alt="image" src="https://github.com/user-attachments/assets/08f69dca-cf0b-4165-ac0b-868b3b781f3b" />

### users

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT | 사용자 ID |
| name | VARCHAR(50) | 사용자 이름 |
| role | VARCHAR | 사용자 역할: CREATOR, STUDENT |
| created_at | DATETIME | 생성 일시 |
| updated_at | DATETIME | 수정 일시 |

### courses

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT | 강의 ID |
| creator_id | BIGINT | 강의를 개설한 크리에이터 ID |
| title | VARCHAR(100) | 강의 제목 |
| description | TEXT | 강의 설명 |
| price | INT | 강의 가격 |
| capacity | INT | 최대 수강 인원 |
| enrolled_count | INT | 현재 좌석을 점유한 신청 인원 |
| start_at | DATETIME | 수강 시작 일시 |
| end_at | DATETIME | 수강 종료 일시 |
| status | VARCHAR | 강의 상태: DRAFT, OPEN, CLOSED |
| created_at | DATETIME | 생성 일시 |
| updated_at | DATETIME | 수정 일시 |

### enrollments

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT | 수강 신청 ID |
| student_id | BIGINT | 수강생 ID |
| course_id | BIGINT | 강의 ID |
| status | VARCHAR | 신청 상태: PENDING, CONFIRMED, CANCELLED |
| confirmed_at | DATETIME | 결제 확정 일시 |
| cancelled_at | DATETIME | 수강 취소 일시 |
| created_at | DATETIME | 생성 일시 |
| updated_at | DATETIME | 수정 일시 |

### 관계

| 관계 | 설명 |
| --- | --- |
| users 1 : N courses | 한 명의 크리에이터는 여러 강의를 개설할 수 있습니다. |
| users 1 : N enrollments | 한 명의 수강생은 여러 수강 신청을 생성할 수 있습니다. |
| courses 1 : N enrollments | 하나의 강의에는 여러 수강 신청이 생성될 수 있습니다. |

---

## 테스트 실행 방법

### 전체 테스트 실행

1. IntelliJ IDEA에서는 `CourseServiceTest`, `EnrollmentServiceTest`, `EnrollmentConcurrencyTest` 테스트 클래스를 각각 실행할 수 있습니다.
  
2. 전체 테스트는 Gradle 명령어로 실행할 수 있습니다.
```bash
./gradlew test
```

---

### 테스트 구성

| 테스트 클래스 | 검증 범위 |
| --- | --- |
| `CourseServiceTest` | 강의 등록, 강의 상태 변경, 강의 목록 조회, 강의 상세 조회 |
| `EnrollmentServiceTest` | 수강 신청, 결제 확정, 수강 취소, 내 수강 신청 목록 조회, 강의별 수강생 조회 |
| `EnrollmentConcurrencyTest` | 동시에 여러 명이 마지막 자리에 신청하는 상황에서 정원 초과 방지 |

---


### CourseServiceTest

| 구분 | 테스트 내용 |
| --- | --- |
| 강의 등록 | 등록 성공, 권한 없음, 존재하지 않는 사용자 |
| 강의 상태 변경 | DRAFT → OPEN 변경 성공, OPEN → CLOSED 변경 성공, DRAFT → CLOSED 변경 실패 |
| 강의 상태 변경 권한 | CREATOR가 아닌 사용자 실패, 강의 개설자가 아닌 크리에이터 실패, 존재하지 않는 사용자 실패 |
| 강의 상태 변경 예외 | 존재하지 않는 강의 상태 변경 실패 |
| 강의 목록 조회 | 전체 조회, OPEN 필터 조회, DRAFT 필터 조회, CLOSED 필터 조회 |
| 강의 목록 조회 예외 | 조건에 맞는 강의가 없으면 빈 목록 반환 |
| 강의 상세 조회 | 상세 조회 성공, 존재하지 않는 강의 조회 실패 |

---

### EnrollmentServiceTest

| 구분 | 테스트 내용 |
| --- | --- |
| 수강 신청 | 신청 성공, 권한 없음, 존재하지 않는 사용자, 존재하지 않는 강의, OPEN 상태 아님 |
| 중복 신청 | PENDING 신청 중복 실패, CONFIRMED 신청 중복 실패, CANCELLED 이후 재신청 성공 |
| 정원 관리 | 정원 마감 시 수강 신청 실패 |
| 결제 확정 | 확정 성공, 권한 없음, 존재하지 않는 사용자, 존재하지 않는 수강 신청 |
| 결제 확정 예외 | 본인 신청 아님, 이미 CONFIRMED 상태, CANCELLED 상태 확정 실패 |
| 수강 취소 | PENDING 취소 성공, CONFIRMED 취소 성공 |
| 수강 취소 예외 | 권한 없음, 존재하지 않는 사용자, 존재하지 않는 수강 신청, 본인 신청 아님 |
| 취소 기간 제한 | 이미 취소된 신청 재취소 실패, 결제 확정 후 취소 가능 기간 초과 실패 |
| 내 수강 신청 목록 조회 | 페이지네이션 조회 성공, 조회 결과 없으면 빈 목록 반환 |
| 내 수강 신청 목록 조회 예외 | 존재하지 않는 사용자 실패, STUDENT가 아닌 사용자 실패 |
| 강의별 수강생 목록 조회 | 크리에이터가 본인 강의의 확정 수강생 목록 조회 성공 |
| 강의별 수강생 목록 조회 예외 | 존재하지 않는 사용자, CREATOR가 아닌 사용자, 존재하지 않는 강의, 본인 강의 아님 |
| 강의별 수강생 목록 조회 결과 | 확정 수강생이 없으면 빈 목록 반환 |
---

###  EnrollmentConcurrencyTest (동시성 테스트)

- `EnrollmentConcurrencyTest`에서 마지막 자리 동시 신청 상황을 검증했습니다.

| 상황 | 기대 결과 |
| --- | --- |
| 정원 10명, 기존 신청 9명, 20명 동시 신청 | 1명만 성공 |
| 정원 10명, 기존 신청 5명, 20명 동시 신청 | 5명만 성공 |

검증 내용:

- 성공 수가 남은 좌석 수와 일치하는지 확인
- 실패 요청이 모두 `COURSE_CAPACITY_FULL` 예외인지 확인
- 예상하지 못한 예외가 없는지 확인
- `enrolledCount`가 정원을 초과하지 않는지 확인
- 실제 저장된 수강 신청 수가 정원을 초과하지 않는지 확인

