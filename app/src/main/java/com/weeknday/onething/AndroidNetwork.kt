package com.weeknday.onething

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

class AndroidNetwork(private val m_cCtx: Context) : BroadcastReceiver() {
    private var m_strNetworkType = ""
    private var m_strNetworkConnType = ""
    private var m_strNetworkClass = ""
    private var m_bIsConnected = false
    private var m_bIsAvailable = false
    private var m_bIsRoaming = false
    private var m_bSetCall = false
    private var m_cConnInfo: ConnectivityManager? = null
    private fun GetNetworkClass(): String {
        var strRes = ""
        var cTelephonyMgr: TelephonyManager? = null
        cTelephonyMgr = m_cCtx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val nNetType = cTelephonyMgr.networkType
        strRes =
            when (nNetType) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                else -> "Unknown"
            }
        m_strNetworkClass = GetNetworkClassToString(nNetType)
        return strRes
    }

    fun GetNetworkClassType(): String {
        return m_strNetworkClass
    }

    private fun GetNetworkClassToString(nNetType: Int): String {
        var strRes = ""
        strRes = when (nNetType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "NETWORK_TYPE_GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "NETWORK_TYPE_EDGE"
            TelephonyManager.NETWORK_TYPE_CDMA -> "NETWORK_TYPE_CDMA"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "NETWORK_TYPE_1xRTT"
            TelephonyManager.NETWORK_TYPE_IDEN -> "NETWORK_TYPE_IDEN"
            TelephonyManager.NETWORK_TYPE_UMTS -> "NETWORK_TYPE_UMTS"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "NETWORK_TYPE_EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "NETWORK_TYPE_EVDO_A"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "NETWORK_TYPE_HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "NETWORK_TYPE_HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "NETWORK_TYPE_HSPA"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "NETWORK_TYPE_EVDO_B"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "NETWORK_TYPE_EHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "NETWORK_TYPE_HSPAP"
            TelephonyManager.NETWORK_TYPE_LTE -> "NETWORK_TYPE_LTE"
            else -> "Unknown"
        }
        return strRes
    }

    private fun InitConnActMgr() {
        if (m_cConnInfo == null) m_cConnInfo =
            m_cCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun GetNetworkConnectInfo() {
        m_strNetworkType = GetNetworkClass()
        InitConnActMgr()
        var cNetInfo = m_cConnInfo!!.activeNetworkInfo
        if (cNetInfo != null) {
            val nNetConnType = cNetInfo.type
            when (nNetConnType) {
                ConnectivityManager.TYPE_MOBILE -> m_strNetworkConnType = "MOBILE"
                ConnectivityManager.TYPE_BLUETOOTH -> m_strNetworkConnType = "BLUETOOTH"
                ConnectivityManager.TYPE_ETHERNET -> m_strNetworkConnType = "ETHERNET"
                ConnectivityManager.TYPE_WIFI -> m_strNetworkConnType = "WIFI"
                ConnectivityManager.TYPE_VPN -> m_strNetworkConnType = "VPN"
            }
            cNetInfo = m_cConnInfo!!.getNetworkInfo(nNetConnType)
            m_bIsConnected = cNetInfo!!.isConnected
            m_bIsAvailable = cNetInfo.isAvailable
            m_bIsRoaming = cNetInfo.isRoaming
        }
        m_bSetCall = true
    }

    override fun onReceive(context: Context, intent: Intent) {
        val strAct = intent.action

        // 네트웍에 변경이 일어났을때 발생하는 부분
        if (strAct == ConnectivityManager.CONNECTIVITY_ACTION) {
            GetNetworkConnectInfo()
        }
    }

    fun GetDeviceNetworkType(): String {
        return m_strNetworkType
    }

    fun GetCurrentConnectNetworkTypeName(): String {
        return if (m_bSetCall) m_strNetworkConnType else ""
    }

    fun IsConnect(): Boolean {
        return if (m_bSetCall) m_bIsConnected else false
    }

    fun IsAvailable(): Boolean {
        return if (m_bSetCall) m_bIsAvailable else false
    }

    fun IsRoaming(): Boolean {
        return if (m_bSetCall) m_bIsRoaming else false
    }

    fun InitSetCall() {
        m_bSetCall = false
        m_bIsConnected = false
        m_bIsAvailable = false
        m_bIsRoaming = false
        m_strNetworkConnType = ""
        m_strNetworkType = ""
        m_strNetworkClass = ""
    }
}