package com.bankx.notification.model.entity;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

/**
 * Сущность, представляющая шаблон электронного письма в MongoDB.
 *
 * <p>Содержит информацию о шаблоне письма, включая тему, тело письма,
 * переменные для подстановки и флаги активности и формата.
 */
public class EmailTemplate {
    private ObjectId id;

    @BsonProperty("templateType")
    private String templateType;

    private String subject;
    private String body;

    private List<String> variables;

    public ObjectId getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

}