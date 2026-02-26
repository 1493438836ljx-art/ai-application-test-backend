package com.example.demo.plugin.repository;

import com.example.demo.common.enums.PluginType;
import com.example.demo.plugin.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {

    List<Plugin> findByType(PluginType type);

    List<Plugin> findByIsActiveTrue();

    List<Plugin> findByTypeAndIsActiveTrue(PluginType type);

    Optional<Plugin> findByName(String name);

    boolean existsByName(String name);
}
