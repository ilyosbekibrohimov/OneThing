package com.weeknday.cheri;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

public class CallStateService extends Service
{
    public String TAG = getClass().getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 서비스에서 가장 먼저 호출됨(최초에 한번만)
        Log.d(TAG, "서비스의 onCreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // 서비스가 호출될 때마다 실행
        Log.d(TAG, "서비스의 onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // 서비스가 종료될 때 실행
        Log.d(TAG, "서비스의 onDestroy");
    }
}
