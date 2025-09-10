package com.bankx.notification.model.dto;

import java.util.Objects;
import java.util.UUID;
import jakarta.validation.constraints.*;

/**
 * DTO класс, представляющий событие регистрации пользователя.
 *
 * <p>Содержит информацию, необходимую для обработки регистрации нового пользователя
 * и отправки соответствующего уведомления (письма активации аккаунта).
 *
 * <p>Для обеспечения корректности данных использует
 * аннотации валидации Jakarta Bean Validation.
 */
public class UserRegistrationEvent {
    private String eventId;

    @NotNull(message = "Ключ активации обязателен")
    private UUID activationKey;

    @NotBlank(message = "Электронная почта обязательна")
    @Size(max = 100, message = "Электронная почта не должна превышать 100 символов")
    @Email(message = "Введите корректный адрес электронной почты")
    private String email;

    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;

    /**
     * Конструктор по умолчанию.
     */
    public UserRegistrationEvent() {
    }

    /**
     * Создает событие регистрации пользователя с указанными данными.
     *
     * @param email         электронная почта пользователя
     * @param firstName     имя пользователя
     * @param lastName      фамилия пользователя
     * @param activationKey ключ активации аккаунта
     */
    public UserRegistrationEvent(String email, String firstName, String lastName, UUID activationKey) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.activationKey = activationKey;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

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

    /**
     * Возвращает строковое представление объекта.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "UserRegistrationEvent{" +
                "eventId='" + eventId + '\'' +
                ", activationKey=" + activationKey +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }

    /**
     * Возвращает хэш-код объекта.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(eventId, activationKey, email, firstName, lastName);
    }

    /**
     * Сравнивает объект с другим объектом на равенство.
     *
     * @param o объект для сравнения
     * @return true если объекты равны, иначе false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRegistrationEvent that = (UserRegistrationEvent) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(activationKey, that.activationKey) &&
                Objects.equals(email, that.email) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName);
    }
}