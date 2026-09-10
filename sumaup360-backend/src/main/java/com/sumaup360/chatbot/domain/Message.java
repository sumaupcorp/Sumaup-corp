package com.sumaup360.chatbot.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Mensaje de una conversacion. role: USER | ASSISTANT. */
@Entity
@Table(name = "message", schema = "chatbot")
@Getter
@Setter
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;
}
