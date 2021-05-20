package com.weeknday.cheri;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;


public class KakaoLogin //extends Activity
{
    private SessionCallback mKakaocallback;

    // Send Data
    private static int    m_nAuthState = HirooTypes.STATUS_UNKNOWN;
    private static String m_strAuthStateMsg = "notlogin";
    private static String m_strAToken = "";
    private static String m_strRToken = "";
    private static String m_strUserName = "";
    private static String m_strUserID = "";
    private static String m_strProfileURL = "";

    private int m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_UNKNOWN;

    private static WebActivity.SendMassgeHandler m_Handler = null;

    private Activity m_Activity = null;
    private AndroidLog	m_Log	= null;

    public KakaoLogin(WebActivity.SendMassgeHandler cHandler, Activity cAct)
    {
        m_Activity  = cAct;
        m_Handler   = cHandler;

        GetAppKeyHash();

        // Allocate AndroidLog Class
        if( m_Log == null )
        {
            m_Log = new AndroidLog();
            m_Log.initialize( "kr.co.hiroo", "Shop.txt", false );
        }

        // 헤쉬키를 가져온다
        GetAppKeyHash();
    }

    public void KakaoLoginStart()
    {
        IsKakaoLogin();     // 카카오 로그인 요청
    }

    public int CheckKakaoSessionState()
    {
        com.kakao.auth.Session.getCurrentSession().checkAndImplicitOpen();
        String strSessionState = String.valueOf(com.kakao.auth.Session.getCurrentSession().checkState());
        strSessionState = strSessionState.toUpperCase();
        if( strSessionState.compareTo("CLOSED") == 0 )
        {
            m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED;
            return m_nStatus;
        }

        m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_SESSIONOPEN;
        return m_nStatus;
    }

    private void IsKakaoLogin()
    {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "IsKakaoLogin : ");

        // 카카오 세션을 오픈한다
        mKakaocallback = new SessionCallback();
        com.kakao.auth.Session.getCurrentSession().addCallback(mKakaocallback);
        com.kakao.auth.Session.getCurrentSession().checkAndImplicitOpen();
        com.kakao.auth.Session.getCurrentSession().open(AuthType.KAKAO_TALK_EXCLUDE_NATIVE_LOGIN, m_Activity);

        String strSessionState = String.valueOf(com.kakao.auth.Session.getCurrentSession().checkState());
        strSessionState = strSessionState.toUpperCase();
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : Session State : %s", strSessionState);
        if( strSessionState.compareTo("CLOSED") == 0 )
        {
            if( m_Handler != null )
            {
                m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED;
                m_Handler.sendEmptyMessage(m_nStatus);
            }
        }
    }

    public void Logout()
    {
        UserManagement.requestUnlink(new UnLinkResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e(errorResult.toString());
                m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED;
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                //redirectLoginActivity();
                m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED;
            }

            @Override
            public void onNotSignedUp() {
                //redirectSignupActivity();
                m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED;
            }

            @Override
            public void onSuccess(Long userId) {
                //redirectLoginActivity();
                m_nStatus = HirooTypes.STATUS_LOGIN_KAKAO_LOGOUT_SUCCESS;
            }
        });
    }

    public int GetLastStatus()
    {
        return m_nStatus;
    }

    private class SessionCallback implements ISessionCallback
    {
        @Override
        public void onSessionOpened()
        {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onSessionOpened");
            m_strAToken = com.kakao.auth.Session.getCurrentSession().getAccessToken();
            m_strRToken = com.kakao.auth.Session.getCurrentSession().getRefreshToken();

            // 사용자 정보를 가져옴, 회원가입 미가입시 자동가입 시킴
            KakaoRequestMe();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception)
        {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onSessionOpenFailed");

            if(exception != null)
            {
                Log.d("TAG" , exception.getMessage());

                m_nAuthState = HirooTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED;
                m_strAuthStateMsg = exception.getMessage();

                m_Log.write(AndroidLog.LOGTYPE_INFO, "KakaoException : %s", m_strAuthStateMsg);

                if( m_Handler != null )
                    m_Handler.sendEmptyMessage(HirooTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED);
            }
        }
    }

    /**
     * 사용자의 상태를 알아 보기 위해 me API 호출을 한다.
     */
    protected void KakaoRequestMe()
    {
        UserManagement.requestMe(new MeResponseCallback()
        {
            @Override
            public void onFailure(ErrorResult errorResult)
            {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onFailure");

                m_nAuthState = errorResult.getErrorCode();
                m_strAuthStateMsg = errorResult.getErrorMessage();

                m_Log.write(AndroidLog.LOGTYPE_INFO, "Error Code : %d", m_nAuthState);
                m_Log.write(AndroidLog.LOGTYPE_INFO, "Error Msg : %d", m_strAuthStateMsg);

                int ClientErrorCode = -777;
                if( m_nAuthState == ClientErrorCode )
                    m_strAuthStateMsg = "카카오톡 서버의 네트워크가 불안정합니다.";

                if( m_Handler != null )
                {
                    m_Handler.sendEmptyMessage(HirooTypes.STATUS_LOGIN_KAKAO_COMPLETE);
                }
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult)
            {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onSessionClosed");

                m_nAuthState = errorResult.getErrorCode();
                m_strAuthStateMsg = errorResult.getErrorMessage();

                m_Log.write(AndroidLog.LOGTYPE_INFO, "Error Code : %d", m_nAuthState);
                m_Log.write(AndroidLog.LOGTYPE_INFO, "Error Msg : %d", m_strAuthStateMsg);

                if( m_Handler != null )
                    m_Handler.sendEmptyMessage(HirooTypes.STATUS_LOGIN_KAKAO_COMPLETE);
            }

            @Override
            public void onSuccess(UserProfile userProfile)
            {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onSuccess");

                m_nAuthState = 0;
                m_strAuthStateMsg = "success";

                m_strUserID = String.valueOf(userProfile.getId());
                m_strUserName = userProfile.getNickname();
                m_strProfileURL = userProfile.getProfileImagePath();

                if( m_Handler != null )
                    m_Handler.sendEmptyMessage(HirooTypes.STATUS_LOGIN_KAKAO_COMPLETE);
            }

            @Override
            public void onNotSignedUp()
            {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onNotSignedUp");

                // 자동가입이 아닐경우 동의창
                if( m_Handler != null )
                    m_Handler.sendEmptyMessage(HirooTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP);
            }
        });
    }

    private void GetAppKeyHash()
    {
        try
        {
            PackageInfo info = m_Activity.getPackageManager().getPackageInfo(m_Activity.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures)
            {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.d("Hash key", something);
            }
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }
    }

    public String ResultData()
    {
        //SetTimeout(-1);

        // 현재(2017.03.14) Kakao는 Email 정보를 얻을 수 없다.
        // "Status","StatusMessage","AccessToken","RefreshToken","Google ID","Email","DisplayName"
        String strRes = "";
        strRes = m_nAuthState + "," + m_strAuthStateMsg + "," + m_strAToken + "," + m_strRToken + "," + m_strUserID + ","
                + "" + "," + m_strUserName;

        return strRes;
    }

    public String ResultDataForJson()
    {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try
        {
            jsonWrite.put( "AccInfo", "kakao" );
            jsonWrite.put( "Status", m_nAuthState );
            jsonWrite.put( "StatusMsg", m_strAuthStateMsg );
            jsonWrite.put( "AccessToken", m_strAToken );
            jsonWrite.put( "RefreshToken", m_strRToken );
            jsonWrite.put( "ID", m_strUserID );
            jsonWrite.put( "EMail", "" );
            jsonWrite.put( "Name", m_strUserName );

            strRes = jsonWrite.toString();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return strRes;
    }

    /* nTimeout : 초 단위, -1 이면 Timeout 제한 없음. */
    public void SetTimeout(int nTimeout )
    {
        int nCurTime = 0;
        while(m_nStatus != HirooTypes.STATUS_LOGIN_KAKAO_COMPLETE )
        {
            if( nCurTime > nTimeout )
                break;

            Sleep(1000);
            nCurTime++;
        }
    }

    public void Sleep(int nTime)
    {
        try
        {
            Thread.sleep(nTime);
        }
        catch( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
}
