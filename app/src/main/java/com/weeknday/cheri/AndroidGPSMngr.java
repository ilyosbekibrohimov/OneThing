package com.weeknday.cheri;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

// Location 제공자에서 정보를 얻어오기(GPS)
// 1. Location을 사용하기 위한 권한을 얻어와야한다 AndroidManifest.xml
//     ACCESS_FINE_LOCATION : NETWORK_PROVIDER, GPS_PROVIDER
//     ACCESS_COARSE_LOCATION : NETWORK_PROVIDER
// 2. LocationManager 를 통해서 원하는 제공자의 리스너 등록
// 3. GPS 는 에뮬레이터에서는 기본적으로 동작하지 않는다
// 4. 실내에서는 GPS_PROVIDER 를 요청해도 응답이 없다.  특별한 처리를 안하면 아무리 시간이 지나도
//    응답이 없다.
//    해결방법은
//     ① 타이머를 설정하여 GPS_PROVIDER 에서 일정시간 응답이 없는 경우 NETWORK_PROVIDER로 전환
//     ② 혹은, 둘다 한꺼번헤 호출하여 들어오는 값을 사용하는 방식.

public class AndroidGPSMngr extends Service implements LocationListener {
    final public int GPSMGR_STATUS_UNKNOWN = -1;    // 알수 없음
    final public int GPSMGR_STATUS_COMPLETE_RECEIVE = 0;    // 수신 완료
    final public int GPSMGR_STATUS_INITIALIZE = 1;    // 초기화 완료
    final public int GPSMGR_STATUS_RECEIVE_STANDBY = 2;    // 수신 대기
    final public int GPSMGR_STATUS_RECEIVE = 3;    // 수신 중
    final public int GPSMGR_STATUS_NOT_RECEIVE = 4;    // 미 수신중
    final public int GPSMGR_STATUS_NOT_PROVIDER = 97;    // 수신할 네트워크 또는 GPS가 없음.
    final public int GPSMGR_STATUS_NOT_INITIALIZE = 98;    // 초기화 하지 않음
    final public int GPSMGR_STATUS_EXCEPTION = 99;    // Exception 발생

    final public int MY_PERMISSIONS_REQUEST_GPS = 3;

    private int m_nRecvStatus = GPSMGR_STATUS_NOT_INITIALIZE;

    private String m_strErrMsg = "";
    private String m_strGPSInfo = "";

    private boolean m_bGPSEnabled = false;
    private boolean m_bNetworkEnabled = false;

    private Handler mHandler;
    private final Activity mAct;

    private LocationManager m_cLocMgr = null;
    private Location        m_cLoc = null;

    private double m_dLongitude = 0.0;
    private double m_dLatitude  = 0.0;

    private boolean m_bRunDlg = false;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public AndroidGPSMngr(Activity cAct, Handler chandler) {
        this.mAct = cAct;
        this.mHandler = chandler;

        m_nRecvStatus = GPSMGR_STATUS_INITIALIZE;

        GetLocationInfo();
    }

    public String GetLastErrorMsg() {
        return m_strErrMsg;
    }

    public int GetCurrentStatus() {
        return m_nRecvStatus;
    }

    public boolean IsUseGPS() {
        return this.m_bGPSEnabled;
    }

    public String GetGPSInfo()
    {
        if( m_cLoc != null )
        {
            //m_strGPSInfo = "Longitude : " + m_cLoc.getLongitude() + " Latitude : " + m_cLoc.getLatitude();
            m_strGPSInfo = m_cLoc.getLongitude() + "," + m_cLoc.getLatitude();
        }
        else
        {
            //m_strGPSInfo = "Longitude : " + m_dLongitude + " Latitude : " + m_dLatitude;
            //m_strGPSInfo = m_dLongitude + "," + m_dLatitude;    // 마지막 정보를 보내면 GPS 정보를 이용하는 측에서 정보가 이상할 수 있다.
            m_strGPSInfo = "0.0,0.0";
        }

        return m_strGPSInfo;
    }

    public void Update()
    {
        GetLocationInfo();
    }

    public Location GetLocationInfo()
    {
        if (mAct == null)
        {
            m_nRecvStatus = GPSMGR_STATUS_NOT_INITIALIZE;
            return null;
        }

        try
        {
            // LocationManager 객체를 얻어온다
            m_cLocMgr = (LocationManager) mAct.getSystemService(Context.LOCATION_SERVICE);

            if( m_cLocMgr != null )
            {
                // GPS 프로바이더 사용가능여부
                m_bGPSEnabled = m_cLocMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
                Log.d("AndroidGPSMngr", "m_bGPSEnabled : " + m_bGPSEnabled);

                // 네트워크 프로바이더 사용가능여부
                m_bNetworkEnabled = m_cLocMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                Log.d("AndroidGPSMngr", "m_bNetworkEnabled : " + m_bNetworkEnabled);

                m_nRecvStatus = GPSMGR_STATUS_RECEIVE_STANDBY;

                if (m_bNetworkEnabled)
                {
                    if (ActivityCompat.checkSelfPermission(mAct, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(mAct, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        if( (ActivityCompat.shouldShowRequestPermissionRationale(mAct, android.Manifest.permission.ACCESS_FINE_LOCATION)) &&
                                (ActivityCompat.shouldShowRequestPermissionRationale(mAct, android.Manifest.permission.ACCESS_COARSE_LOCATION)) )
                        {
                            // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                            // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

                        }
                        else
                        {
                            ActivityCompat.requestPermissions(mAct, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_GPS);
                            // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
                        }

                        return null;
                    }

                    m_cLocMgr.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
                            100, // 통지사이의 최소 시간간격 (miliSecond)
                            1, // 통지사이의 최소 변경거리 (m)
                            this);

                    if( m_cLocMgr != null )
                    {
                        m_cLoc = m_cLocMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if( m_cLoc != null )
                        {
                            m_dLongitude    = m_cLoc.getLongitude();
                            m_dLatitude     = m_cLoc.getLatitude();
                        }
                    }
                }
                else
                {
                    if( m_bGPSEnabled )
                    {
                        // GPS 제공자의 정보가 바뀌면 콜백하도록 리스너 등록
                        m_cLocMgr.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                                100, // 통지사이의 최소 시간간격 (miliSecond)
                                1, // 통지사이의 최소 변경거리 (m)
                                this);

                        if( m_cLocMgr != null )
                        {
                            m_cLoc = m_cLocMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if( m_cLoc != null )
                            {
                                m_dLongitude    = m_cLoc.getLongitude();
                                m_dLatitude     = m_cLoc.getLatitude();
                            }
                        }
                    }
                    else
                        m_nRecvStatus = GPSMGR_STATUS_NOT_PROVIDER;
                }
            }
        }
        catch( Exception e )
        {
            m_nRecvStatus = GPSMGR_STATUS_EXCEPTION;
            m_strErrMsg = e.getMessage();
        }

		return m_cLoc;
	}


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch(requestCode) {
            case MY_PERMISSIONS_REQUEST_GPS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // 권한 허가
                    // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                    GetLocationInfo();

                }
                else
                {
                    // 권한 거부
                    // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
                }

                break;
        }
    }

    public double GetLatitude()
    {
        if( m_cLoc != null )
        {
            m_dLatitude = m_cLoc.getLatitude();
        }

        return m_dLatitude;
    }

    public double GetLongitude()
    {
        if( m_cLoc != null )
        {
            m_dLongitude = m_cLoc.getLongitude();
        }

        return m_dLongitude;
    }

    public void StopUsingGPS()
    {
        if( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission( mAct, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
        {
            return;
        }

        if( m_cLocMgr != null )
        {
            m_cLocMgr.removeUpdates(AndroidGPSMngr.this);
        }
    }

    public boolean SetUsingGPS()
    {
        if( mAct != null )
        {
            AlertDialog.Builder cAlertDlg = new AlertDialog.Builder(mAct);

            // Setting Dialog Title
            cAlertDlg.setTitle("GPS 설정");

            // Setting Dialog Message
            cAlertDlg.setMessage("GPS 설정되지 않았습니다. GPS를 설정 하시겠습니까?");

            // On pressing Settings button
            cAlertDlg.setPositiveButton("설정", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog,int which)
                {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    mAct.startActivity(intent);

                    m_bRunDlg = true;
                }
            });

            // on pressing cancel button
            cAlertDlg.setNegativeButton("취소", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                    m_bRunDlg = false;
                }
            });

            // Showing Alert Message
            cAlertDlg.show();
        }

        return m_bRunDlg;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        //여기서 위치값이 갱신되면 이벤트가 발생한다.
        //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.

        Log.d("AndroidGPSMngr", "onLocationChanged, location:" + location);
        double	dLongitude	= location.getLongitude();	//경도
        double	dLatitude	= location.getLatitude();	//위도
        double	dAltitude	= location.getAltitude();	//고도
        float	fAccuracy	= location.getAccuracy();	//정확도
        String	strProvider	= location.getProvider();	//위치제공자

        //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
        //Network 위치제공자에 의한 위치변화
        //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
        Log.d("AndroidGPSMngr", "위치정보 : " + strProvider + "\n위도 : " + dLongitude + "\n경도 : " + dLatitude + "\n고도 : " + dAltitude + "\n정확도 : "  + fAccuracy);
        m_strGPSInfo = "위치정보 : " + strProvider + "\n위도 : " + dLongitude + "\n경도 : " + dLatitude + "\n고도 : " + dAltitude + "\n정확도 : "  + fAccuracy;

        m_nRecvStatus = GPSMGR_STATUS_COMPLETE_RECEIVE;
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        // Disabled시
        Log.d("AndroidGPSMngr", "onProviderDisabled, provider:" + provider);
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        // Enabled시
        Log.d("AndroidGPSMngr", "onProviderEnabled, provider:" + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        // 변경시
        Log.d("AndroidGPSMngr", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
    }
}