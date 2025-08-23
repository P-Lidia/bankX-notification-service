package com.bankx.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    private Long clientId;

    @Id
    private UUID activationKey;

    @Id
    private int statusId;

    private String eventType;
    private String email;
    private String phone;
    private String messageContent;
    private String sendingChannel;
    private LocalDateTime createdAt;
    private String errorMessage;

}
