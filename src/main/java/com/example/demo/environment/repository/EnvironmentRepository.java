package com.example.demo.environment.repository;

import com.example.demo.environment.entity.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Page<Environment> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Environment> findByIsActiveTrue();

    Optional<Environment> findByName(String name);

    boolean existsByName(String name);
}
