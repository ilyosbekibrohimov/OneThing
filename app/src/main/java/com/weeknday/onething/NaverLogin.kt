package com.weeknday.onething

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.view.WindowManager
import android.widget.Toast
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.OAuthLoginDefine
import com.nhn.android.naverlogin.OAuthLoginHandler
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton
import com.weeknday.onething.WebActivity.SendMassgeHandler
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class NaverLogin(cHandler: SendMassgeHandler?, cAct: Activity?) : Activity() {
    /**
     * client 정보를 넣어준다.
     */
    private val OAUTH_CLIENT_ID = "edH91lvffH7UBUTHlZQu"
    private val OAUTH_CLIENT_SECRET = "H781dSrHPv"
    private val OAUTH_CLIENT_NAME = "네이버 아이디로 로그인"
    private var mOAuthLoginInstance: OAuthLogin? = null
    private var m_cContext: Context? = null
    private var m_cActivity: Activity? = null
    private val mOAuthLoginButton: OAuthLoginButton? = null
    private var m_bAuthResult = false
    private var m_strAuthState = ""
    private var m_nAuthState = OneThingTypes.STATUS_UNKNOWN
    private var m_strAuthStateMsg = "notlogin"
    private var m_strID = ""
    private var m_strAToken = ""
    private var m_strRToken = ""
    private var m_strEmail = ""
    private var m_strName = ""
    var mHandler: SendMassgeHandler? = null
    private fun initData() {
        mOAuthLoginInstance = OAuthLogin.getInstance()
        mOAuthLoginInstance.init(m_cContext, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME)
        /*
         * 2015년 8월 이전에 등록하고 앱 정보 갱신을 안한 경우 기존에 설정해준 callback intent url 을 넣어줘야 로그인하는데 문제가 안생긴다.
         * 2015년 8월 이후에 등록했거나 그 뒤에 앱 정보 갱신을 하면서 package name 을 넣어준 경우 callback intent url 을 생략해도 된다.
         */
        //mOAuthLoginInstance.init(mContext, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME, OAUTH_callback_intent_url);
    }

    override fun onResume() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        super.onResume()
    }

    /**
     * startOAuthLoginActivity() 호출시 인자로 넘기거나, OAuthLoginButton 에 등록해주면 인증이 종료되는 걸 알 수 있다.
     */
    private val mOAuthLoginHandler: OAuthLoginHandler = object : OAuthLoginHandler() {
        override fun run(success: Boolean) {
            m_bAuthResult = success
            m_strAuthState = mOAuthLoginInstance!!.getLastErrorCode(m_cContext).code
            m_strAuthStateMsg = mOAuthLoginInstance!!.getLastErrorDesc(m_cContext)
            if (m_bAuthResult) {
                m_strAToken = mOAuthLoginInstance!!.getAccessToken(m_cContext)
                m_strRToken = mOAuthLoginInstance!!.getRefreshToken(m_cContext)
                val expiresAt = mOAuthLoginInstance!!.getExpiresAt(m_cContext)
                val tokenType = mOAuthLoginInstance!!.getTokenType(m_cContext)

                // 간단하게 Thread 생성자만으로 스레드 실행
                Thread { // TODO Auto-generated method stub
                    getProfile(m_strAToken)
                    if (mHandler != null) {
                        m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE
                        mHandler!!.sendEmptyMessage(m_nAuthState)
                    }
                }.start()
            } else {
                if (m_strAuthState.compareTo("user cancel") == 0) {
                    if (mHandler != null) {
                        m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL
                        mHandler!!.sendEmptyMessage(m_nAuthState)
                    }
                }
                Toast.makeText(m_cContext, "errorCode:$m_strAuthState, errorDesc:$m_strAuthStateMsg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun Login() {
        mOAuthLoginInstance!!.startOauthLoginActivity(m_cActivity, mOAuthLoginHandler)
    }

    fun GetRefreshToken() {
        RefreshTokenTask().execute()
    }

    fun Verifier() {
        RequestApiTask().execute()
    }

    fun Logout() {
        //mOAuthLoginInstance.logout(m_cContext);
        m_nAuthState = if (mOAuthLoginInstance!!.logoutAndDeleteToken(m_cContext)) OneThingTypes.STATUS_LOGIN_NAVER_LOGOUT_SUCCESS else OneThingTypes.STATUS_LOGIN_NAVER_LOGOUT_FAILED
    }

    fun GetLastStatus(): Int {
        return m_nAuthState
    }

    fun DeleteToken() {
        DeleteTokenTask().execute()
    }

    private inner class DeleteTokenTask : AsyncTask<Void?, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Void?): Void? {
            val isSuccessDeleteToken = mOAuthLoginInstance!!.logoutAndDeleteToken(m_cContext)
            if (!isSuccessDeleteToken) {
                // 서버에서 token 삭제에 실패했어도 클라이언트에 있는 token 은 삭제되어 로그아웃된 상태이다
                // 실패했어도 클라이언트 상에 token 정보가 없기 때문에 추가적으로 해줄 수 있는 것은 없음
            }
            if (mHandler != null) {
                m_nAuthState = OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN
                mHandler!!.sendEmptyMessage(m_nAuthState)
            }
            return null
        }
    }

    private inner class RequestApiTask : AsyncTask<Void?, Void?, String>() {
        protected override fun doInBackground(vararg params: Void?): String? {
            //String url = "https://openapi.naver.com/v1/nid/getUserProfile.xml";
            val url = "https://openapi.naver.com/v1/nid/me"
            val at = mOAuthLoginInstance!!.getAccessToken(m_cContext)
            return mOAuthLoginInstance!!.requestApi(m_cContext, at, url)
        }
    }

    private inner class RefreshTokenTask : AsyncTask<Void?, Void?, String>() {
        protected override fun doInBackground(vararg params: Void?): String? {
            return mOAuthLoginInstance!!.refreshAccessToken(m_cContext)
        }
    }

    fun ResultData(): String {
        // "Status","StatusMessage","AccessToken","RefreshToken","Google ID","Email","DisplayName"
        var strRes = ""
        if (m_strAuthState == "" || m_strAuthState.isEmpty()) {
            m_strAuthState = "00"
        } else {
            m_strAuthState.length
        }
        strRes = (m_strAuthState + "," + m_strAuthStateMsg + "," + m_strAToken + "," + m_strRToken + "," + m_strID + ","
                + m_strEmail + "," + m_strName)
        return strRes
    }

    fun ResultDataForJson(): String {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("AccInfo", "naver")
            jsonWrite.put("Status", m_strAuthState)
            jsonWrite.put("StatusMsg", m_strAuthStateMsg)
            jsonWrite.put("AccessToken", m_strAToken)
            jsonWrite.put("RefreshToken", m_strRToken)
            jsonWrite.put("ID", m_strID)
            jsonWrite.put("EMail", m_strEmail)
            jsonWrite.put("Name", m_strName)
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return strRes
    }

    fun getProfile(strAToken: String) {
        //String token = "YOUR_ACCESS_TOKEN";// 네이버 로그인 접근 토큰;
        val header = "bearer $strAToken" // Bearer 다음에 공백 추가
        try {
            val apiURL = "https://openapi.naver.com/v1/nid/me"
            val url = URL(apiURL)
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Authorization", header)
            val responseCode = con.responseCode
            val br: BufferedReader
            br = if (responseCode == 200) { // 정상 호출
                BufferedReader(InputStreamReader(con.inputStream))
            } else {  // 에러 발생
                BufferedReader(InputStreamReader(con.errorStream))
            }
            var inputLine: String?
            val response = StringBuffer()
            while (br.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            br.close()
            println(response.toString())
            // {"resultcode":"00","message":"success","response":{"email":"mfohs@naver.com","nickname":"\u3160\u3160","enc_id":"bb190836ef21fd1d73e4d98ae82fe4d33eab1bddd273c68cd47c125723726a08","profile_image":"https:\/\/phinf.pstatic.net\/contactthumb\/profile\/blog\/32\/1\/mfohs.jpg?type=s80","age":"30-39","gender":"M","id":"12203770","name":"\uc624\ud604\uc11d","birthday":"12-17"}}
            try {
                val jsonObj = JSONObject(response.toString())
                val strMsg = jsonObj.getString("message")
                if (strMsg.compareTo("success") == 0) {
                    val strResponseData = jsonObj.getString("response")
                    val jsonRes = JSONObject(strResponseData)
                    m_strID = jsonRes.getString("id")
                    m_strName = jsonRes.getString("name")
                    m_strEmail = jsonRes.getString("email")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun SetHandler(handler: SendMassgeHandler?) {
        mHandler = handler
    }

    init {
        OAuthLoginDefine.DEVELOPER_VERSION = true
        mHandler = cHandler
        m_cContext = cAct
        m_cActivity = cAct
        initData()
        Login()
    }
}