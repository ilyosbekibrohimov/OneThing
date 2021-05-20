package com.weeknday.cheri;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.widget.Toast;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginDefine;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NaverLogin extends Activity
{
    /**
     * client 정보를 넣어준다.
     */
    private String OAUTH_CLIENT_ID = "edH91lvffH7UBUTHlZQu";
    private String OAUTH_CLIENT_SECRET = "H781dSrHPv";
    private String OAUTH_CLIENT_NAME = "네이버 아이디로 로그인";

    private OAuthLogin mOAuthLoginInstance;
    private Context m_cContext = null;
    private Activity m_cActivity = null;

    private OAuthLoginButton mOAuthLoginButton;

    private boolean m_bAuthResult = false;
    private String  m_strAuthState = "";
    private int     m_nAuthState = OneThingTypes.STATUS_UNKNOWN;
    private String  m_strAuthStateMsg = "notlogin";

    private String m_strID = "";
    private String m_strAToken = "";
    private String m_strRToken = "";
    private String m_strEmail = "";
    private String m_strName = "";

    public WebActivity.SendMassgeHandler mHandler = null;

    public NaverLogin(WebActivity.SendMassgeHandler cHandler, Activity cAct)
    {
        OAuthLoginDefine.DEVELOPER_VERSION = true;

        mHandler = cHandler;
        m_cContext  = cAct;
        m_cActivity = cAct;

        InitData();

        Login();
    }


    private void InitData()
    {
        mOAuthLoginInstance = OAuthLogin.getInstance();
        mOAuthLoginInstance.init(m_cContext, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME);
		/*
		 * 2015년 8월 이전에 등록하고 앱 정보 갱신을 안한 경우 기존에 설정해준 callback intent url 을 넣어줘야 로그인하는데 문제가 안생긴다.
		 * 2015년 8월 이후에 등록했거나 그 뒤에 앱 정보 갱신을 하면서 package name 을 넣어준 경우 callback intent url 을 생략해도 된다.
		 */
        //mOAuthLoginInstance.init(mContext, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME, OAUTH_callback_intent_url);
    }

    @Override
    protected void onResume()
    {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onResume();
    }

    /**
     * startOAuthLoginActivity() 호출시 인자로 넘기거나, OAuthLoginButton 에 등록해주면 인증이 종료되는 걸 알 수 있다.
     */
    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler()
    {
        @Override
        public void run(boolean success)
        {
            m_bAuthResult = success;
            m_strAuthState = mOAuthLoginInstance.getLastErrorCode(m_cContext).getCode();
            m_strAuthStateMsg = mOAuthLoginInstance.getLastErrorDesc(m_cContext);

            if( m_bAuthResult )
            {
                m_strAToken = mOAuthLoginInstance.getAccessToken(m_cContext);
                m_strRToken = mOAuthLoginInstance.getRefreshToken(m_cContext);
                long expiresAt = mOAuthLoginInstance.getExpiresAt(m_cContext);
                String tokenType = mOAuthLoginInstance.getTokenType(m_cContext);

                // 간단하게 Thread 생성자만으로 스레드 실행
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // TODO Auto-generated method stub
                        getProfile(m_strAToken);

                        if( mHandler != null )
                        {
                            m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE;
                            mHandler.sendEmptyMessage(m_nAuthState);
                        }
                    }
                }).start();
            }
            else
            {
                if( m_strAuthState.compareTo("user cancel") == 0 )
                {
                    if( mHandler != null )
                    {
                        m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL;
                        mHandler.sendEmptyMessage(m_nAuthState);
                    }
                }

                Toast.makeText(m_cContext, "errorCode:" + m_strAuthState + ", errorDesc:" + m_strAuthStateMsg, Toast.LENGTH_SHORT).show();
            }
        };
    };

    public void Login()
    {
        mOAuthLoginInstance.startOauthLoginActivity(m_cActivity, mOAuthLoginHandler);
    }

    public void GetRefreshToken()
    {
        new RefreshTokenTask().execute();
    }

    public void Verifier()
    {
        new RequestApiTask().execute();
    }

    public void Logout()
    {
        //mOAuthLoginInstance.logout(m_cContext);
        if( mOAuthLoginInstance.logoutAndDeleteToken(m_cContext) )
            m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_LOGOUT_SUCCESS;
        else
            m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_LOGOUT_FAILED;
    }

    public int GetLastStatus()
    {
        return m_nAuthState;
    }

    public void DeleteToken()
    {
        new DeleteTokenTask().execute();
    }

    private class DeleteTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            boolean isSuccessDeleteToken = mOAuthLoginInstance.logoutAndDeleteToken(m_cContext);

            if (!isSuccessDeleteToken) {
                // 서버에서 token 삭제에 실패했어도 클라이언트에 있는 token 은 삭제되어 로그아웃된 상태이다
                // 실패했어도 클라이언트 상에 token 정보가 없기 때문에 추가적으로 해줄 수 있는 것은 없음
            }

            if( mHandler != null )
            {
                m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN;
                mHandler.sendEmptyMessage(m_nAuthState);
            }

            return null;
        }
    }

    private class RequestApiTask extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... params) {
            //String url = "https://openapi.naver.com/v1/nid/getUserProfile.xml";
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = mOAuthLoginInstance.getAccessToken(m_cContext);
            return mOAuthLoginInstance.requestApi(m_cContext, at, url);
        }
    }

    private class RefreshTokenTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return mOAuthLoginInstance.refreshAccessToken(m_cContext);
        }
    }

    public String ResultData()
    {
        // "Status","StatusMessage","AccessToken","RefreshToken","Google ID","Email","DisplayName"
        String strRes = "";
        if( m_strAuthState == "" || m_strAuthState.isEmpty() || m_strAuthState.length() == 0 )
            m_strAuthState = "00";
        strRes = m_strAuthState + "," + m_strAuthStateMsg + "," + m_strAToken + "," + m_strRToken + "," + m_strID + ","
                + m_strEmail + "," + m_strName;
        return strRes;
    }

    public String ResultDataForJson()
    {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try
        {
            jsonWrite.put( "AccInfo", "naver" );
            jsonWrite.put( "Status", m_strAuthState );
            jsonWrite.put( "StatusMsg", m_strAuthStateMsg );
            jsonWrite.put( "AccessToken", m_strAToken );
            jsonWrite.put( "RefreshToken", m_strRToken );
            jsonWrite.put( "ID", m_strID );
            jsonWrite.put( "EMail", m_strEmail );
            jsonWrite.put( "Name", m_strName );

            strRes = jsonWrite.toString();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return strRes;
    }

    public void getProfile(String strAToken)
    {
        //String token = "YOUR_ACCESS_TOKEN";// 네이버 로그인 접근 토큰;
        String header = "bearer " + strAToken; // Bearer 다음에 공백 추가
        try {
            String apiURL = "https://openapi.naver.com/v1/nid/me";
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", header);
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200)
            { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
            else
            {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            System.out.println(response.toString());
            // {"resultcode":"00","message":"success","response":{"email":"mfohs@naver.com","nickname":"\u3160\u3160","enc_id":"bb190836ef21fd1d73e4d98ae82fe4d33eab1bddd273c68cd47c125723726a08","profile_image":"https:\/\/phinf.pstatic.net\/contactthumb\/profile\/blog\/32\/1\/mfohs.jpg?type=s80","age":"30-39","gender":"M","id":"12203770","name":"\uc624\ud604\uc11d","birthday":"12-17"}}
            try
            {
                JSONObject jsonObj = new JSONObject(response.toString());

                String strMsg = jsonObj.getString("message");

                if( strMsg.compareTo("success") == 0 )
                {
                    String strResponseData = jsonObj.getString("response");

                    JSONObject jsonRes = new JSONObject(strResponseData);

                    m_strID     = jsonRes.getString("id");
                    m_strName   = jsonRes.getString("name");
                    m_strEmail	= jsonRes.getString("email");
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void SetHandler(WebActivity.SendMassgeHandler handler)
    {
        mHandler = handler;
    }
}
