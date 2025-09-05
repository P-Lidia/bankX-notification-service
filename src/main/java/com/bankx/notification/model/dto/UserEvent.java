package com.bankx.notification.model.dto;

import java.util.Objects;
import java.util.UUID;

public class UserEvent {
    private String eventId;
    private UUID activationKey;
    private String email;
    private String firstName;
    private String lastName;

    public UserEvent() {
    }

/*    // todo возможно придется переделать название параметра activationKey
    public UserEvent(UUID activationKey) {
        this.activationKey = activationKey;
    }*/

    //todo если понадобится полный конструктор
    public UserEvent(String email, String firstName, String lastName, UUID activationKey) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.activationKey = activationKey;
    }

    public String getEventId() { return eventId; }

    public void setEventId(String eventId) { this.eventId = eventId; }

    public UUID getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(UUID activationKey) {
        this.activationKey = activationKey;
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

    @Override
    public String toString() {
        return "UserEvent{" +
               "activationKey=" + activationKey +
               ", email='" + email + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(activationKey, email, firstName, lastName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEvent userEvent = (UserEvent) o;
        return Objects.equals(activationKey, userEvent.activationKey) &&
               Objects.equals(email, userEvent.email) &&
               Objects.equals(firstName, userEvent.firstName) &&
               Objects.equals(lastName, userEvent.lastName);
    }
}