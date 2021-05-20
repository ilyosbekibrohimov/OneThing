package com.weeknday.onething;
import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.kakao.auth.KakaoSDK;


public class GlobalApplication extends Application {
    private static GlobalApplication mInstance;

    public static GlobalApplication getGlobalApplicationContext() {
        if(mInstance == null)
            throw new IllegalStateException("this application does not inherit GlobalApplication");
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        KakaoSDK.init(new KakaoSDKAdapter());
    }
}

