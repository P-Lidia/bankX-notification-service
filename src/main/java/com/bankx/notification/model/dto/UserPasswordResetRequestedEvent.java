package com.bankx.notification.model.dto;
import com.bankx.notification.model.UserEventType;

public class UserPasswordResetRequestedEvent {
    private String eventId;                 // для идемпотентности (может быть null)
    private UserEventType type;             // МОЖЕТ отсутствовать в двух-топиковой схеме
    private String email;
    private String firstName;
    private String lastName;
    private String locale;                  // "ru" | "en" | "ar" (если null → "en")
    private String resetUrl;                // ссылка от Auth

    public UserPasswordResetRequestedEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public UserEventType getType() { return type; }
    public void setType(UserEventType type) { this.type = type; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getResetUrl() { return resetUrl; }
    public void setResetUrl(String resetUrl) { this.resetUrl = resetUrl; }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName;
        String ln = lastName  == null ? "" : lastName;
        return (fn + " " + ln).trim();
    }
}

