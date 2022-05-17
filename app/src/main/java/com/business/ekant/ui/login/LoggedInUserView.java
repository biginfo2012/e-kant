package com.business.ekant.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    private String token;
    private String fieldName;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayName, String token, String fieldName) {
        this.displayName = displayName;
        this.token = token;
        this.fieldName = fieldName;
    }

    String getDisplayName() {
        return displayName;
    }
    String getToken() {
        return token;
    }
    String getFieldName(){
        return fieldName;
    }
}