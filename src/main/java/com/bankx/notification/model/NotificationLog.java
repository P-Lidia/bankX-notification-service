package com.bankx.notification.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class NotificationLog {
    private Long clientId;
    private UUID activationKey;
    private int statusId;
    private String eventType;
    private String email;
    private String phone;
    private String messageContent;
    private String sendingChannel;
    private LocalDateTime createdAt;
    private String errorMessage;

    // Конструкторы
    public NotificationLog() {
    }

    public NotificationLog(Long clientId, UUID activationKey, int statusId, String eventType,
                           String email, String phone, String messageContent, String sendingChannel,
                           LocalDateTime createdAt, String errorMessage) {
        this.clientId = clientId;
        this.activationKey = activationKey;
        this.statusId = statusId;
        this.eventType = eventType;
        this.email = email;
        this.phone = phone;
        this.messageContent = messageContent;
        this.sendingChannel = sendingChannel;
        this.createdAt = createdAt;
        this.errorMessage = errorMessage;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public UUID getActivationKey() { return activationKey; }
    public void setActivationKey(UUID activationKey) { this.activationKey = activationKey; }

    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }

    public String getSendingChannel() { return sendingChannel; }
    public void setSendingChannel(String sendingChannel) { this.sendingChannel = sendingChannel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        com.bankx.notification.model.NotificationLog that = (com.bankx.notification.model.NotificationLog) o;
        return statusId == that.statusId &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(activationKey, that.activationKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, activationKey, statusId);
    }

    @Override
    public String toString() {
        return "NotificationLog{" +
                "clientId=" + clientId +
                ", activationKey=" + activationKey +
                ", statusId=" + statusId +
                ", eventType='" + eventType + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", messageContent='" + messageContent + '\'' +
                ", sendingChannel='" + sendingChannel + '\'' +
                ", createdAt=" + createdAt +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
