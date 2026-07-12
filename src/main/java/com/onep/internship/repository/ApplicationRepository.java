package com.onep.internship.repository;

import com.onep.internship.model.Application;
import com.onep.internship.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStatus(ApplicationStatus status);
}
