package com.peertopeer.repository;

import com.peertopeer.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.peertopeer.records.UserSummary;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
     Optional<Users> findByUsername(String username);

     List<UserSummary> findByUsernameContaining(String username);

     @Query("SELECT u " +
             "FROM Users u " +
             "WHERE LOWER(u.username) LIKE LOWER(CONCAT(:query, '%')) " +
             "AND u.id != :id")
     Page<UserSummary> searchUsernamesStartingWith(Long id, String query, Pageable pageable);
}
