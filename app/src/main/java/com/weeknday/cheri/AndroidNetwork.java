package com.weeknday.cheri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;



public class AndroidNetwork extends BroadcastReceiver
{
    private Context m_cCtx = null;

    private String m_strNetworkType = "";
    private String m_strNetworkConnType = "";
    private String m_strNetworkClass = "";
    private Boolean m_bIsConnected  = false;
    private Boolean m_bIsAvailable  = false;
    private Boolean m_bIsRoaming    = false;

    private Boolean m_bSetCall = false;

    private ConnectivityManager m_cConnInfo = null;

    public AndroidNetwork(Context cCtx)
    {
        m_cCtx = cCtx;
    }

    private String GetNetworkClass()
    {
        String strRes = "";

        TelephonyManager cTelephonyMgr = null;
        cTelephonyMgr = (TelephonyManager)m_cCtx.getSystemService(Context.TELEPHONY_SERVICE);

        int nNetType = cTelephonyMgr.getNetworkType();
        switch(nNetType)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                strRes = "2G";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                strRes = "3G";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                strRes = "4G";
                break;
            default:
                strRes = "Unknown";
        }

        m_strNetworkClass = GetNetworkClassToString(nNetType);

        return strRes;
    }

    public String GetNetworkClassType()
    {
        return m_strNetworkClass;
    }

    private String GetNetworkClassToString(int nNetType)
    {
        String strRes = "";

        switch(nNetType)
        {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                strRes = "NETWORK_TYPE_GPRS";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                strRes = "NETWORK_TYPE_EDGE";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                strRes = "NETWORK_TYPE_CDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                strRes = "NETWORK_TYPE_1xRTT";
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                strRes = "NETWORK_TYPE_IDEN";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                strRes = "NETWORK_TYPE_UMTS";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                strRes = "NETWORK_TYPE_EVDO_0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                strRes = "NETWORK_TYPE_EVDO_A";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                strRes = "NETWORK_TYPE_HSDPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                strRes = "NETWORK_TYPE_HSUPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                strRes = "NETWORK_TYPE_HSPA";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                strRes = "NETWORK_TYPE_EVDO_B";
                break;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                strRes = "NETWORK_TYPE_EHRPD";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                strRes = "NETWORK_TYPE_HSPAP";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                strRes = "NETWORK_TYPE_LTE";
                break;
            default:
                strRes = "Unknown";
        }

        return strRes;
    }

    private void InitConnActMgr()
    {
        if( m_cConnInfo == null )
            m_cConnInfo = (ConnectivityManager)m_cCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void GetNetworkConnectInfo()
    {
        m_strNetworkType = GetNetworkClass();

        InitConnActMgr();

        NetworkInfo cNetInfo = m_cConnInfo.getActiveNetworkInfo();

        if( cNetInfo != null )
        {
            int nNetConnType = cNetInfo.getType();
            switch(nNetConnType)
            {
                case ConnectivityManager.TYPE_MOBILE:
                    m_strNetworkConnType = "MOBILE";
                    break;
                case ConnectivityManager.TYPE_BLUETOOTH:
                    m_strNetworkConnType = "BLUETOOTH";
                    break;
                case ConnectivityManager.TYPE_ETHERNET:
                    m_strNetworkConnType = "ETHERNET";
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    m_strNetworkConnType = "WIFI";
                    break;
                case ConnectivityManager.TYPE_VPN:
                    m_strNetworkConnType = "VPN";
                    break;
            }

            cNetInfo = m_cConnInfo.getNetworkInfo(nNetConnType);

            m_bIsConnected  = cNetInfo.isConnected();
            m_bIsAvailable  = cNetInfo.isAvailable();
            m_bIsRoaming    = cNetInfo.isRoaming();
        }

        m_bSetCall = true;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String strAct = intent.getAction();

        // 네트웍에 변경이 일어났을때 발생하는 부분
        if (strAct.equals(ConnectivityManager.CONNECTIVITY_ACTION))
        {
            GetNetworkConnectInfo();
        }
    }

    public String GetDeviceNetworkType()
    {
        return m_strNetworkType;
    }

    public String GetCurrentConnectNetworkTypeName()
    {
        if( m_bSetCall )
            return m_strNetworkConnType;

        return "";
    }

    public Boolean IsConnect()
    {
        if( m_bSetCall )
            return m_bIsConnected;

        return false;
    }

    public Boolean IsAvailable()
    {
        if( m_bSetCall )
            return m_bIsAvailable;

        return false;
    }

    public Boolean IsRoaming()
    {
        if( m_bSetCall )
            return m_bIsRoaming;

        return false;
    }

    public void InitSetCall()
    {
        m_bSetCall = false;

        m_bIsConnected  = false;
        m_bIsAvailable  = false;
        m_bIsRoaming    = false;

        m_strNetworkConnType = "";
        m_strNetworkType = "";
        m_strNetworkClass = "";
    }
}
