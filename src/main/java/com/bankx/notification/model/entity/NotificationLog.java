package com.bankx.notification.model.entity;


import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationLog {
    private ObjectId id;

    private String eventId;

    @BsonProperty("event_type")
    private String eventType;

    private String email;

    @BsonProperty("first_name")
    private String firstName;

    @BsonProperty("last_name")
    private String lastName;

    @BsonProperty("activation_key")
    private UUID activationKey;

    @BsonProperty("reset_token")
    private String resetToken;

    @BsonProperty("created_at")
    private LocalDateTime createdAt;

    private String status;

    @BsonProperty("error_message")
    private String errorMessage;

    @BsonProperty("attempt_count")
    private int attemptCount;

    public NotificationLog() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
        this.attemptCount = 0;
    }

    public NotificationLog(UUID activationKey, String eventType,
                           String email, LocalDateTime createdAt, String errorMessage) {
        this.activationKey = activationKey;
        this.eventType = eventType;
        this.email = email;
        this.createdAt = createdAt;
        this.errorMessage = errorMessage;
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UUID getActivationKey() { return activationKey; }
    public void setActivationKey(UUID activationKey) { this.activationKey = activationKey; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public void incrementAttemptCount() { this.attemptCount++; }
}
