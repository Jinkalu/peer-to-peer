package com.peertopeer.entity;


import com.peertopeer.enums.ConversationStatus;
import com.peertopeer.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
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

    private String conversationName;

    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    private Boolean readStatus;
    private Boolean isPinned;

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
        this.status = ConversationStatus.ACTIVE;
        this.readStatus = false;
        this.isPinned = false;
        this.createdAt = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
