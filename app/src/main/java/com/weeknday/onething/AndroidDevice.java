package com.weeknday.onething;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.RequiresApi;

public class AndroidDevice {
    // Device Information
    private static String m_strSDKVersion = "";
    private static String m_strModel = "";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public AndroidDevice(Activity cAct) {
        GetDeviceInformation();
        DeviceGraphicInformation(cAct);
    }

    private void DeviceGraphicInformation(Activity cAct) {
        DisplayMetrics cDisplayMetrics = new DisplayMetrics();
        cAct.getWindowManager().getDefaultDisplay().getMetrics(cDisplayMetrics);


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void GetDeviceInformation() {
        m_strSDKVersion = Build.VERSION.SDK;
        m_strModel = Build.MODEL;


    }

    public String GetSDKVersion() {
        return m_strSDKVersion;
    }




    public String GetModel() {
        return m_strModel;
    }

}