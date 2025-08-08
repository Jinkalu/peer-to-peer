package com.peertopeer.repository;

import com.peertopeer.entity.UserGroupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupSettingsRepository extends JpaRepository<UserGroupSettings, Long> {
  }