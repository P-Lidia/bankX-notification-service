package com.bankx.notification.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.UUID;

/**
 * DTO класс, представляющий событие сброса пароля пользователя.
 *
 * <p>Содержит информацию, необходимую для обработки запроса на сброс пароля
 * и отправки соответствующего уведомления (письма со ссылкой для сброса пароля).
 *
 * <p>Для обеспечения корректности данных использует
 * аннотации валидации Jakarta Bean Validation.
 */
public class UserResetPasswordEvent {

    @NotBlank(message = "Электронная почта обязательна")
    @Size(max = 100, message = "Электронная почта не должна превышать 100 символов")
    @Email(message = "Введите корректный адрес электронной почты")
    private String email;

    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;

    @NotNull(message = "Токен для сброса пароля обязателен")
    private UUID resetToken;

    /**
     * Конструктор по умолчанию.
     */
    public UserResetPasswordEvent() {
    }

    /**
     * Создает событие сброса пароля пользователя с указанными данными.
     *
     * @param email      электронная почта пользователя
     * @param firstName  имя пользователя
     * @param lastName   фамилия пользователя
     * @param resetToken токен для сброса пароля
     */
    public UserResetPasswordEvent(String email, String firstName, String lastName, UUID resetToken) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.resetToken = resetToken;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UUID getResetToken() {
        return resetToken;
    }

    /**
     * Возвращает строковое представление объекта.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "UserResetPasswordEvent{" +
                "email='" + email + '\'' +
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
        return Objects.hash(email, firstName, lastName, resetToken);
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
        return Objects.equals(email, userEvent.email) &&
                Objects.equals(firstName, userEvent.firstName) &&
                Objects.equals(lastName, userEvent.lastName) &&
                Objects.equals(resetToken, userEvent.resetToken);
    }
}