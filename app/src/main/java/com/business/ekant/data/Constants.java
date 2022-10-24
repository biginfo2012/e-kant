package com.business.ekant.data;

public class Constants {

    public static final String SHARE_PREF = "StepLogin";
    public static final String SHARE_MAIN = "Main";
    public static final String SHARE_EMAIL = "Email";
    public static final String SHARE_PWD = "Password";
    public static final String TOKEN = "Token";
    public static final String FTOKEN = "FToken";
    public static final String USER_NAME = "User Name";
    public static final String USER_PLACE = "Field";
    public static final String USER_STIME = "User_Stime";
    public static final String USER_SendPos = "USER_SendPos";
    public static final String GET_SHIFT = "GET_SHIFT";
    public static final String USER_LTIME = "User_Ltime";
    public static final String SHIFT_ID = "Shift_Id";
    public static final String TODAY_SHIFT_ID = "Today_Shift_Id";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String STAFF_ADDRESS = "STAFF_ADDRESS";
    public static final String SHIFT_ADDRESS = "SHIFT_ADDRESS";
    public static final String FIELD_NAME = "FIELD_NAME";


    public static final String REMOTE_TYPE = "Remote_Type";
    public static final String REMOTE_SHIFT_ID = "Remote_Shift_Id";
    public static final String REMOTE_FIELD_NAME = "Remote_Field_Name";
    public static final String REMOTE_FIELD_TEL = "Remote_Field_Tel";
    public static final String REMOTE_ADMIN_TEL = "Remote_Admin_Tel";
    public static final String REMOTE_EMERGENCY_TEL = "Remote_Emergency_Tel";
    public static final String REMOTE_SHIFT_TIME = "Remote_Shift_Time";
    public static final String REMOTE_TITLE = "Remote_Title";
    public static final String REMOTE_BODY = "Remote_Body";


    public static final String BASE_URL = "https://e-kant.j-tm.jp/";
    //public static final String BASE_URL = "http://218.251.240.209/";
    //public static final String BASE_URL = "http://192.168.2.116/";
    public static final String LOGIN_URL = BASE_URL + "api/v1/client/login";
    public static final String LOGOUT_URL = BASE_URL + "api/v1/client/logout";
    public static final String GET_SHIFT_LIST = BASE_URL + "api/v1/client/get-shift-list";
    public static final String UPDATE_DEVICE_TOKEN = BASE_URL + "api/v1/client/update-device-token";
    public static final String UPDATE_SHIFT_LIST = BASE_URL + "api/v1/client/update-shift-list";
    public static final String CONFIRM_START = BASE_URL + "api/v1/client/confirm-start";
    public static final String CONFIRM_YESTERDAY = BASE_URL + "api/v1/client/confirm-yesterday";
    public static final String CONFIRM_TODAY = BASE_URL + "api/v1/client/confirm-today";
    public static final String CONFIRM_ARRIVE = BASE_URL + "api/v1/client/confirm-arrive";
    public static final String CONFIRM_LEAVE = BASE_URL + "api/v1/client/confirm-leave";
    public static final String REQUEST_EARLY_LEAVE = BASE_URL + "api/v1/client/request-early-leave";
    public static final String REQUEST_REST = "api/v1/client/request-rest";

}
