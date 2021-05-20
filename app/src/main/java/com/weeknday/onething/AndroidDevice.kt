package com.weeknday.onething

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi

class AndroidDevice @RequiresApi(api = Build.VERSION_CODES.O) constructor(cAct: Activity) {
    private fun DeviceGraphicInformation(cAct: Activity) {
        val cDisplayMetrics = DisplayMetrics()
        cAct.windowManager.defaultDisplay.getMetrics(cDisplayMetrics)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun GetDeviceInformation() {
        m_strSDKVersion = Build.VERSION.SDK
        m_strModel = Build.MODEL
    }

    fun getSdkVersion(): String {
        return m_strSDKVersion
    }

    fun GetModel(): String {
        return m_strModel
    }

    companion object {
        // Device Information
        private var m_strSDKVersion = ""
        private var m_strModel = ""
    }

    init {
        GetDeviceInformation()
        DeviceGraphicInformation(cAct)
    }
}