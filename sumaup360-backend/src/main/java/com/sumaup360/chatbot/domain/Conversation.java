package com.sumaup360.chatbot.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Conversacion del chatbot Suma (por usuario). */
@Entity
@Table(name = "conversation", schema = "chatbot")
@Getter
@Setter
public class Conversation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", length = 160)
    private String title;
}
