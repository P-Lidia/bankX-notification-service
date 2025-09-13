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

    @BsonProperty("isActive")
    private Boolean isActive;

    @BsonProperty("isHtml")
    private Boolean isHtml;

    @BsonProperty("createdAt")
    private Date createdAt;

    @BsonProperty("updatedAt")
    private Date updatedAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsHtml() {
        return isHtml;
    }

    public void setIsHtml(Boolean isHtml) {
        this.isHtml = isHtml;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}