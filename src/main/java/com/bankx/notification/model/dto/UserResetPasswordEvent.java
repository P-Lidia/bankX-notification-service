package com.bankx.notification.model.dto;

import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * DTO класс, представляющий событие сброса пароля пользователя.
 *
 * <p>Содержит информацию, необходимую для обработки запроса на сброс пароля
 * и отправки соответствующего уведомления (письма со ссылкой для сброса пароля).
 *
 * <p>Для обеспечения корректности данных в классе используются
 * аннотации валидации Jakarta Bean Validation.
 */
public class UserResetPasswordEvent {
    private String eventId;

    @NotBlank(message = "Электронная почта обязательна")
    @Size(max = 100, message = "Электронная почта не должна превышать 100 символов")
    @Email(message = "Введите корректный адрес электронной почты")
    private String email;

    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;

    @NotBlank(message = "Токен для сброса пароля обязателен")
    @Size(min = 16, max = 128, message = "Токен для сброса пароля должен быть длиной от 16 до 128 символов")
    private String resetToken;

    /**
     * Конструктор по умолчанию.
     */
    public UserResetPasswordEvent() {
    }

    /**
     * Создает событие сброса пароля пользователя с указанными данными.
     *
     * @param eventId    идентификатор события
     * @param email      электронная почта пользователя
     * @param firstName  имя пользователя
     * @param lastName   фамилия пользователя
     * @param resetToken токен для сброса пароля
     */
    public UserResetPasswordEvent(String eventId, String email, String firstName,
                                  String lastName, String resetToken) {
        this.eventId = eventId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.resetToken = resetToken;
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

    /**
     * Возвращает строковое представление объекта.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "UserResetPasswordEvent{" + "eventId='" + eventId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", resetToken='" + resetToken + '\'' +
                '}';
    }

    /**
     * Возвращает хэш-код объекта.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(eventId, email, firstName, lastName, resetToken);
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
        UserResetPasswordEvent userEvent = (UserResetPasswordEvent) o;
        return Objects.equals(eventId, userEvent.eventId) &&
                Objects.equals(email, userEvent.email) &&
                Objects.equals(firstName, userEvent.firstName) &&
                Objects.equals(lastName, userEvent.lastName) &&
                Objects.equals(resetToken, userEvent.resetToken);
    }
}