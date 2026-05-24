package com.example.courseenrollment.course.domain;

import com.example.courseenrollment.global.entity.BaseTimeEntity;
import com.example.courseenrollment.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Builder
    public Course(User creator, String title, String description, int price, int capacity, LocalDateTime startAt, LocalDateTime endAt) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
        this.enrolledCount = 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = CourseStatus.DRAFT;
    }

    public void changeStatus(CourseStatus status) {
        this.status = status;
    }
    public void increaseEnrolledCount() {
        this.enrolledCount++;
    }

    public void decreaseEnrolledCount() {
        if (this.enrolledCount > 0) {
            this.enrolledCount--;
        }
    }
}
