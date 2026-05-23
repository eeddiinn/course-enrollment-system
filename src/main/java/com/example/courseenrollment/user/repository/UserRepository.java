package com.example.courseenrollment.user.repository;

import com.example.courseenrollment.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
