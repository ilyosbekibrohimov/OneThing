package com.weeknday.onething

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.weeknday.onething.WebActivity.SendMassgeHandler
import org.json.JSONException
import org.json.JSONObject

class GoogleActivity : FragmentActivity(), GoogleApiClient.OnConnectionFailedListener,
    View.OnClickListener {

    private val RC_SIGN_IN = 9001
    private var m_lyMain: LinearLayout? = null
    private fun setBtnClickListener() {
        // Button listeners
        findViewById<View>(R.id.sign_in_button).setOnClickListener(this)
        findViewById<View>(R.id.sign_out_button).setOnClickListener(this)
        findViewById<View>(R.id.disconnect_button).setOnClickListener(this)
    }

    private fun setLayoutVisibility() {
        m_lyMain = findViewById<View>(R.id.ly_google_main) as LinearLayout
        m_lyMain?.visibility = View.INVISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_google)
        m_cAct = this

        // Allocate AndroidLog Class
        if (m_Log == null) {
            m_Log = AndroidLog()
            m_Log?.initialize("com.weeknday", "tecq.txt", false)
        }
        if (m_bIsInitCtrl) {
            mStatusTextView = findViewById<View>(R.id.status) as TextView
            setBtnClickListener()
        } else setLayoutVisibility()
        SetGoogleSignInConfig()
        if (!m_bIsLogout) signIn() else SetLogout(false)
    }

    public override fun onStart() {
        super.onStart()
        silentSignIn()
    }

    public override fun onDestroy() {
        super.onDestroy()


    }

    // [START onActivityResult]
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "onActivityResult : ")
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "          Request Code : %d", requestCode)
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "          Result Code : %d", resultCode)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    // [END handleSignInResult]
    private fun SetGoogleSignInConfig() {
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "SetGoogleSignInConfig : ")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("259687128945-ernfud0t817eguikblau57gbcu2fe1bq.apps.googleusercontent.com") // oauth2:server:client_id:
            .requestEmail()
            .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso) //.addScope(new Scope(Scopes.PROFILE))
            .build()
    }

    // [START signIn]
    private fun signIn() {
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "signIn : ")
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // [END revokeAccess]
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "onConnectionFailed : ")

        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        val TAG = "GoogleActivity"
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "          : %d", connectionResult.errorCode)
        m_Log?.write(AndroidLog.LOGTYPE_INFO, "          : %s", connectionResult.errorMessage)
        if (mHandler != null) {
            m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED
            mHandler?.sendEmptyMessage(m_nCurStatus)
        }
        m_cAct?.finish()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.sign_in_button -> signIn()
            R.id.sign_out_button -> signOut()
            R.id.disconnect_button -> revokeAccess()
        }
    }

    companion object {

        private var mGoogleApiClient: GoogleApiClient? = null
        private var mStatusTextView: TextView? = null
        private var m_cAct: Activity? = null
        private var m_nAuthState = OneThingTypes.STATUS_UNKNOWN
        private var m_strAuthStateMsg: String? = "notlogin"
        private var m_strID: String? = ""
        private var m_strIdToken: String? = ""
        private var m_strEmail: String? = ""
        private var m_strDisplayName: String? = ""
        private  var mHandler: SendMassgeHandler? = null
        private var m_Log: AndroidLog? = null
        private var m_bIsLogout = false

        // Status Flag in this Activity
        private const val m_bIsInitCtrl = false
        private var m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN
        @JvmStatic
        fun resultData(): String {
            var strRes = ""
            strRes =
                "$m_nAuthState,$m_strAuthStateMsg,$m_strIdToken,,$m_strID,$m_strEmail,$m_strDisplayName"
            return strRes
        }

        @JvmStatic
        fun resultDataForJson(): String {
            var strRes = ""
            val jsonWrite = JSONObject()
            try {
                jsonWrite.put("AccInfo", "google")
                jsonWrite.put("Status", m_nAuthState)
                jsonWrite.put("StatusMsg", m_strAuthStateMsg)
                jsonWrite.put("AccessToken", m_strIdToken)
                jsonWrite.put("RefreshToken", "")
                jsonWrite.put("ID", m_strID)
                jsonWrite.put("EMail", m_strEmail)
                jsonWrite.put("Name", m_strDisplayName)
                strRes = jsonWrite.toString()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return strRes
        }

        @JvmStatic
        fun SetLogout(bIsLogout: Boolean) {
            m_bIsLogout = bIsLogout
        }

        // [END onActivityResult]
        // [START handleSignInResult]
        private fun handleSignInResult(result: GoogleSignInResult?) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "handleSignInResult : ")
            val m_bAuthResult = result!!.isSuccess
            m_nAuthState = result.status.statusCode
            m_strAuthStateMsg = result.status.statusMessage
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          Status Code : %d", m_nAuthState)
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          Status Msg : %s", m_strAuthStateMsg)
            if (m_bAuthResult) {
                // Signed in successfully, show authenticated UI.
                val acct = result.signInAccount
                m_strID = acct!!.id
                m_strIdToken = acct.idToken
                m_strEmail = acct.email
                m_strDisplayName = acct.displayName
                acct.serverAuthCode
                if (m_bIsInitCtrl) mStatusTextView!!.text =
                    acct.displayName
                if (mHandler != null) {
                    m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE
                    mHandler?.sendEmptyMessage(m_nCurStatus)
                }
            } else {
                // Signed out, show unauthenticated UI.
                if (mHandler != null) {
                    if (m_nAuthState == 4) {     // SIGN_IN_REQUIRED
                        m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED
                    } else {
                        m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED
                    }
                    mHandler?.sendEmptyMessage(m_nCurStatus)
                }
            }
            m_cAct!!.finish()
        }

        @JvmStatic
        fun silentSignIn() {
            val opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient)
            if (opr.isDone) {
                val result = opr.get()
                handleSignInResult(result)
            } else {
                showProgressDialog()
                opr.setResultCallback { p0 ->
                    hideProgressDialog()
                    handleSignInResult(p0)
                }
            }
        }

        // [END signIn]
        // [START signOut]
        @JvmStatic
        fun signOut() {
            mGoogleApiClient?.connect()
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback { status ->
                if (status.isSuccess) m_nCurStatus =
                    OneThingTypes.STATUS_LOGIN_GOOGLE_LOGOUT_SUCCESS else m_nCurStatus =
                    OneThingTypes.STATUS_LOGIN_GOOGLE_LOGOUT_FAILED
                if (mHandler != null) {
                    mHandler?.sendEmptyMessage(m_nCurStatus)
                }
            }
        }

        // [END signOut]
        // [START revokeAccess]
        fun revokeAccess() {
            Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback { }
        }

        private fun showProgressDialog() {}
        private fun hideProgressDialog() {}



         fun setHandler(handler: SendMassgeHandler?) {
            mHandler = handler
        }
    }
}