package com.peertopeer.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peertopeer.enums.ConversationStatus;
import com.peertopeer.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "conversations")
public class Conversations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(nullable = true)
    private String conversationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Enumerated(EnumType.STRING)
    private ConversationStatus status = ConversationStatus.ACTIVE; // Default

    private Boolean readStatus = false; // Default
    private Boolean isPinned = false;   // Default

    @Column(updatable = false)
    private Long createdAt;

    @Version
    private Long updatedAt;


    @ManyToMany
    @JoinTable(
            name = "conversation_user",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<Users> users = new HashSet<>();



    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
