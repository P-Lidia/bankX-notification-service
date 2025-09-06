package com.bankx.notification.model.dto;

import java.util.Objects;

public class UserResetPasswordEvent {
    private String eventId;
    private String email;
    private String firstName;
    private String lastName;
    private String resetToken;
    private String resetUrl;

    public UserResetPasswordEvent() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public void setResetUrl(String resetUrl) {
        this.resetUrl = resetUrl;
    }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName;
        String ln = lastName == null ? "" : lastName;
        return (fn + " " + ln).trim();
    }

    @Override
    public String toString() {
        return "UserResetPasswordEvent{" + "eventId='" + eventId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", resetToken='" + resetToken + '\'' +
                ", resetUrl='" + resetUrl + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, email, firstName, lastName, resetToken, resetUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResetPasswordEvent userEvent = (UserResetPasswordEvent) o;
        return Objects.equals(eventId, userEvent.eventId) &&
                Objects.equals(email, userEvent.email) &&
                Objects.equals(firstName, userEvent.firstName) &&
                Objects.equals(lastName, userEvent.lastName) &&
                Objects.equals(resetToken, userEvent.resetToken) &&
                Objects.equals(resetUrl, userEvent.resetUrl);
    }
}