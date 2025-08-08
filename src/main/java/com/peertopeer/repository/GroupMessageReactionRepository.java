package com.peertopeer.repository;

import com.peertopeer.entity.GroupMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMessageReactionRepository extends JpaRepository<GroupMessageReaction, Long> {
}