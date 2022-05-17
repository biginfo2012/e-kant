package com.business.ekant.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private String fieldName;

    public LoggedInUser(String userId, String displayName, String fieldName) {
        this.userId = userId;
        this.displayName = displayName;
        this.fieldName = fieldName;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFieldName() {
        return fieldName;
    }
}