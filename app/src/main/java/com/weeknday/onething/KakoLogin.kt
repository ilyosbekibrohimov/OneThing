package com.weeknday.onething

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.kakao.auth.AuthType
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeResponseCallback
import com.kakao.usermgmt.callback.UnLinkResponseCallback
import com.kakao.usermgmt.response.model.UserProfile
import com.kakao.util.exception.KakaoException
import com.kakao.util.helper.log.Logger
import com.weeknday.onething.WebActivity.SendMassgeHandler
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest

class KakaoLogin(cHandler: SendMassgeHandler?, cAct: Activity?) {
    private var mKakaocallback: SessionCallback? = null
    private var m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_UNKNOWN
    private var m_Activity: Activity? = null
    private var m_Log: AndroidLog? = null
    fun KakaoLoginStart() {
        IsKakaoLogin() // 카카오 로그인 요청
    }

    fun CheckKakaoSessionState(): Int {
        Session.getCurrentSession().checkAndImplicitOpen()
        var strSessionState = Session.getCurrentSession().checkState().toString()
        strSessionState = strSessionState.toUpperCase()
        if (strSessionState.compareTo("CLOSED") == 0) {
            m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED
            return m_nStatus
        }
        m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPEN
        return m_nStatus
    }

    private fun IsKakaoLogin() {
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "IsKakaoLogin : ")

        // 카카오 세션을 오픈한다
        mKakaocallback = SessionCallback()
        Session.getCurrentSession().addCallback(mKakaocallback)
        Session.getCurrentSession().checkAndImplicitOpen()
        Session.getCurrentSession().open(AuthType.KAKAO_TALK_EXCLUDE_NATIVE_LOGIN, m_Activity)
        var strSessionState = Session.getCurrentSession().checkState().toString()
        strSessionState = strSessionState.toUpperCase()
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          : Session State : %s", strSessionState)
        if (strSessionState.compareTo("CLOSED") == 0) {
            if (m_Handler != null) {
                m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED
                m_Handler!!.sendEmptyMessage(m_nStatus)
            }
        }
    }

    fun Logout() {
        UserManagement.requestUnlink(object : UnLinkResponseCallback() {
            override fun onFailure(errorResult: ErrorResult) {
                Logger.e(errorResult.toString())
                m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED
            }

            override fun onSessionClosed(errorResult: ErrorResult) {
                //redirectLoginActivity();
                m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED
            }

            override fun onNotSignedUp() {
                //redirectSignupActivity();
                m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_LOGOUT_FAILED
            }

            override fun onSuccess(userId: Long) {
                //redirectLoginActivity();
                m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_LOGOUT_SUCCESS
            }
        })
    }

    fun GetLastStatus(): Int {
        return m_nStatus
    }

    private inner class SessionCallback : ISessionCallback {
        override fun onSessionOpened() {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onSessionOpened")
            m_strAToken = Session.getCurrentSession().accessToken
            m_strRToken = Session.getCurrentSession().refreshToken

            // 사용자 정보를 가져옴, 회원가입 미가입시 자동가입 시킴
            KakaoRequestMe()
        }

        override fun onSessionOpenFailed(exception: KakaoException) {
            m_Log?.write(AndroidLog.LOGTYPE_INFO, "onSessionOpenFailed")
            Log.d("TAG", exception.message!!)
            m_nAuthState = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED
            m_strAuthStateMsg = exception.message
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "KakaoException : %s", m_strAuthStateMsg)
            if (m_Handler != null) m_Handler!!.sendEmptyMessage(OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED)
        }
    }

    /**
     * 사용자의 상태를 알아 보기 위해 me API 호출을 한다.
     */
    protected fun KakaoRequestMe() {
        UserManagement.requestMe(object : MeResponseCallback() {
            override fun onFailure(errorResult: ErrorResult) {
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "onFailure")
                m_nAuthState = errorResult.errorCode
                m_strAuthStateMsg = errorResult.errorMessage
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "Error Code : %d", m_nAuthState)
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "Error Msg : %d", m_strAuthStateMsg)
                val ClientErrorCode = -777
                if (m_nAuthState == ClientErrorCode) m_strAuthStateMsg = "카카오톡 서버의 네트워크가 불안정합니다."
                if (m_Handler != null) {
                    m_Handler!!.sendEmptyMessage(OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE)
                }
            }

            override fun onSessionClosed(errorResult: ErrorResult) {
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "onSessionClosed")
                m_nAuthState = errorResult.errorCode
                m_strAuthStateMsg = errorResult.errorMessage
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "Error Code : %d", m_nAuthState)
                m_Log?.write(AndroidLog.LOGTYPE_INFO, "Error Msg : %d", m_strAuthStateMsg)
                if (m_Handler != null) m_Handler!!.sendEmptyMessage(OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE)
            }

            override fun onSuccess(userProfile: UserProfile) {
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onSuccess")
                m_nAuthState = 0
                m_strAuthStateMsg = "success"
                m_strUserID = userProfile.id.toString()
                m_strUserName = userProfile.nickname
                m_strProfileURL = userProfile.profileImagePath
                if (m_Handler != null) m_Handler!!.sendEmptyMessage(OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE)
            }

            override fun onNotSignedUp() {
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onNotSignedUp")

                // 자동가입이 아닐경우 동의창
                if (m_Handler != null) m_Handler!!.sendEmptyMessage(OneThingTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP)
            }
        })
    }

    private fun GetAppKeyHash() {
        try {
            val info = m_Activity!!.packageManager.getPackageInfo(
                m_Activity!!.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                var md: MessageDigest
                md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val something = String(Base64.encode(md.digest(), 0))
                Log.d("Hash key", something)
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString())
        }
    }

    fun ResultData(): String {
        var strRes = ""
        strRes =
            (m_nAuthState.toString() + "," + m_strAuthStateMsg + "," + m_strAToken + "," + m_strRToken + "," + m_strUserID + ","
                    + "" + "," + m_strUserName)
        return strRes
    }

    fun ResultDataForJson(): String {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("AccInfo", "kakao")
            jsonWrite.put("Status", m_nAuthState)
            jsonWrite.put("StatusMsg", m_strAuthStateMsg)
            jsonWrite.put("AccessToken", m_strAToken)
            jsonWrite.put("RefreshToken", m_strRToken)
            jsonWrite.put("ID", m_strUserID)
            jsonWrite.put("EMail", "")
            jsonWrite.put("Name", m_strUserName)
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return strRes
    }

    /* nTimeout : 초 단위, -1 이면 Timeout 제한 없음. */
    fun SetTimeout(nTimeout: Int) {
        var nCurTime = 0
        while (m_nStatus != OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE) {
            if (nCurTime > nTimeout) break
            Sleep(1000)
            nCurTime++
        }
    }

    fun Sleep(nTime: Int) {
        try {
            Thread.sleep(nTime.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        // Send Data
        private var m_nAuthState = OneThingTypes.STATUS_UNKNOWN
        private var m_strAuthStateMsg: String? = "notlogin"
        private var m_strAToken = ""
        private var m_strRToken = ""
        private var m_strUserName = ""
        private var m_strUserID = ""
        private var m_strProfileURL = ""
        private var m_Handler: SendMassgeHandler? = null
    }

    init {
        m_Activity = cAct
        m_Handler = cHandler
        GetAppKeyHash()

        // Allocate AndroidLog Class
        if (m_Log == null) {
            m_Log = AndroidLog()
            m_Log?.initialize("kr.co.hiroo", "Shop.txt", false)
        }

        // 헤쉬키를 가져온다
        GetAppKeyHash()
    }
}