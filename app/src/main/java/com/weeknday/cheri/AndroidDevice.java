package com.weeknday.cheri;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

public class AndroidDevice {
    // Device Information
    private static String m_strSDKVersion = "";
    private static String m_strSDKCodeName = "";
    private static String m_strModel = "";

    public AndroidDevice(Activity cAct) {
        GetDeviceInformation();
        DeviceGraphicInformation(cAct);
    }

    private void DeviceGraphicInformation(Activity cAct) {
        DisplayMetrics cDisplayMetrics = new DisplayMetrics();
        cAct.getWindowManager().getDefaultDisplay().getMetrics(cDisplayMetrics);

        // Device Graphic Information
        float m_fDensity = cDisplayMetrics.density;
        int m_nWidthPx = cDisplayMetrics.widthPixels;
        int m_nHeightPx = cDisplayMetrics.heightPixels;
        int m_nDensityDpi = cDisplayMetrics.densityDpi;

        Log.d("AndroidDevice", "Density : " + m_fDensity);
        Log.d("AndroidDevice", "WidthPx : " + m_nWidthPx);
        Log.d("AndroidDevice", "HeightPx : " + m_nHeightPx);
        Log.d("AndroidDevice", "DensityDpi : " + m_nDensityDpi);
    }

    private void GetDeviceInformation() {
        m_strSDKVersion = Build.VERSION.SDK;
        m_strSDKCodeName = Build.VERSION.CODENAME;
        String m_strReleaseVersion = Build.VERSION.RELEASE;
        String m_strBrand = Build.BRAND;
        String m_strDevice = Build.DEVICE;
        String m_strDisplay = Build.DISPLAY;
        String m_strID = Build.ID;
        m_strModel = Build.MODEL;
        String m_strManufacturer = Build.MANUFACTURER;
        String m_strProduct = Build.PRODUCT;

        Log.d("AndroidDevice", "SDKVersion : " + m_strSDKVersion);
        Log.d("AndroidDevice", "SDKCodeName : " + m_strSDKCodeName);
        Log.d("AndroidDevice", "ReleaseVersion : " + m_strReleaseVersion);
        Log.d("AndroidDevice", "Brand : " + m_strBrand);
        Log.d("AndroidDevice", "Device : " + m_strDevice);
        Log.d("AndroidDevice", "Display : " + m_strDisplay);
        Log.d("AndroidDevice", "ID : " + m_strID);
        Log.d("AndroidDevice", "Model : " + m_strModel);
        Log.d("AndroidDevice", "Manufacturer : " + m_strManufacturer);
        Log.d("AndroidDevice", "Product : " + m_strProduct);
    }

    public String GetSDKVersion() {
        return m_strSDKVersion;
    }

    public String GetSDKCodeName() {
        return m_strSDKCodeName;
    }


    public String GetModel() {
        return m_strModel;
    }

}