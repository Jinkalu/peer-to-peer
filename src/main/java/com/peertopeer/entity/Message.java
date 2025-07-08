package com.peertopeer.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderUUID;

//    private String receiverUUID; // Nullable for group chats

    @Column(nullable = false, length = 2000) // Increased length for long messages
    private String message;

    private String mediaURL;
    private String reaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SEND; // Default

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private Boolean isLiked = false;

    @Column(updatable = false)
    private Long createdAt;

    private Long updatedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    private Conversations conversation;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().toEpochMilli();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }


}
