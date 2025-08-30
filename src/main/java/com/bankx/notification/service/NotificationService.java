package com.bankx.notification.service;

import com.bankx.notification.model.dto.UserEvent;
import com.bankx.notification.repository.NotificationLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationService {

    //todo бизнес  логика  уведомлений - сервис, который не отправляет но знает че мы там делаем при отправке
    @Inject
    private EmailService emailService;

    @Inject
    private NotificationLogRepository repository;

    public void processUserActivation(UserEvent userEvent) {
        String activationLink = "http://bankx.com/activate?key=" + userEvent.getActivationKey();
        emailService.sendActivationEmail(userEvent.getEmail(), activationLink);
    }
}
