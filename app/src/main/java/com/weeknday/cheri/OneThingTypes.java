package com.weeknday.cheri;

public class OneThingTypes
{
    // Handler Message Status Definitions
    public static final int STATUS_UNKNOWN = -99999;



    // Kakao Account
    public static final int STATUS_LOGIN_KAKAO_COMPLETE = 1000;
    public static final int STATUS_LOGIN_KAKAO_NOTSIGNEDUP = 1002;
    public static final int STATUS_LOGIN_KAKAO_SESSIONOPEN = 1003;
    public static final int STATUS_LOGIN_KAKAO_SESSIONCLOSED = 1004;
    public static final int STATUS_LOGIN_KAKAO_LOGOUT_SUCCESS = 1500;
    public static final int STATUS_LOGIN_KAKAO_LOGOUT_FAILED = 1501;
    public static final int STATUS_LOGIN_KAKAO_SESSIONOPENFAILED = 1998;
    public static final int STATUS_LOGIN_KAKAO_UNKNOWN = 1999;

    public static final int STATUS_LOGIN_GOOGLE_COMPLETE = 2000;
    public static final int STATUS_LOGIN_GOOGLE_SIGNINREQUIRED = 2001;
    public static final int STATUS_LOGIN_GOOGLE_LOGOUT_SUCCESS = 2500;
    public static final int STATUS_LOGIN_GOOGLE_LOGOUT_FAILED = 2501;
    public static final int STATUS_LOGIN_GOOGLE_ONDESTROY = 2996;
    public static final int STATUS_LOGIN_GOOGLE_AUTHFAILED = 2997;
    public static final int STATUS_LOGIN_GOOGLE_CONNECTIONFAILED = 2998;
    public static final int STATUS_LOGIN_GOOGLE_UNKNOWN = 2999;

    public static final int STATUS_LOGIN_NAVER_COMPLETE = 3000;
    public static final int STATUS_LOGIN_NAVER_USERCANCEL = 3001;
    public static final int STATUS_LOGIN_NAVER_DELETETOKEN = 3002;
    public static final int STATUS_LOGIN_NAVER_LOGOUT_SUCCESS = 3500;
    public static final int STATUS_LOGIN_NAVER_LOGOUT_FAILED = 3501;
    public static final int STATUS_LOGIN_NAVER_UNKNOWN = 3999;
}