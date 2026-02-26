package com.example.demo.prompt.repository;

import com.example.demo.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    Page<Prompt> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Prompt> findByName(String name);

    boolean existsByName(String name);
}
