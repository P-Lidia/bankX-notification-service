package com.bankx.notification.model.dto;

public class UserPasswordResetRequestedEvent {

    private String eventId;                 // для идемпотентности (может быть null)
    private String email;
    private String firstName;
    private String lastName;
    private String resetUrl;                // ссылка от Auth

    public UserPasswordResetRequestedEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getResetUrl() { return resetUrl; }
    public void setResetUrl(String resetUrl) { this.resetUrl = resetUrl; }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName;
        String ln = lastName  == null ? "" : lastName;
        return (fn + " " + ln).trim();
    }
}
