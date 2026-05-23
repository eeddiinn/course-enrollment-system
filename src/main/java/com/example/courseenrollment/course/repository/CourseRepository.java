package com.example.courseenrollment.course.repository;

import com.example.courseenrollment.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
