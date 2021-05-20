package com.weeknday.onething;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private final int RC_SIGN_IN = 9001;

    private static GoogleApiClient mGoogleApiClient;

    private final static Handler mPosthandler = new Handler();

    private static TextView mStatusTextView = null;
    private LinearLayout m_lyMain = null;
    private static Activity m_cAct = null;

    private static int m_nAuthState = OneThingTypes.STATUS_UNKNOWN;
    private static String m_strAuthStateMsg = "notlogin";

    private static String m_strID = "";
    private static String m_strIdToken = "";
    private static String m_strEmail = "";
    private static String m_strDisplayName = "";

    public static WebActivity.SendMassgeHandler mHandler = null;

    private static AndroidLog m_Log = null;
    private static Boolean m_bIsLogout = false;

    // Status Flag in this Activity
    private static final boolean m_bIsInitCtrl = false;
    private static int m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN;



    private void SetBtnClickListener() {
        // Button listeners
        findViewById(com.weeknday.onething.R.id.sign_in_button).setOnClickListener(this);
        findViewById(com.weeknday.onething.R.id.sign_out_button).setOnClickListener(this);
        findViewById(com.weeknday.onething.R.id.disconnect_button).setOnClickListener(this);
    }

    private void SetLayoutVisibility() {
        m_lyMain = (LinearLayout) findViewById(com.weeknday.onething.R.id.ly_google_main);
        m_lyMain.setVisibility(View.INVISIBLE);
    }

    public static String ResultData() {
        String strRes = "";
        strRes = m_nAuthState + "," + m_strAuthStateMsg + "," + m_strIdToken + "," + "" + "," + m_strID + "," + m_strEmail + "," + m_strDisplayName;
        return strRes;
    }

    public static String ResultDataForJson() {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("AccInfo", "google");
            jsonWrite.put("Status", m_nAuthState);
            jsonWrite.put("StatusMsg", m_strAuthStateMsg);
            jsonWrite.put("AccessToken", m_strIdToken);
            jsonWrite.put("RefreshToken", "");
            jsonWrite.put("ID", m_strID);
            jsonWrite.put("EMail", m_strEmail);
            jsonWrite.put("Name", m_strDisplayName);

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return strRes;
    }

    public static void SetLogout(Boolean bIsLogout) {
        m_bIsLogout = bIsLogout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.weeknday.onething.R.layout.act_google);

        m_cAct = this;

        // Allocate AndroidLog Class
        if (m_Log == null) {
            m_Log = new AndroidLog();
            m_Log.initialize("com.weeknday", "tecq.txt", false);
        }

        if (m_bIsInitCtrl){
            mStatusTextView = (TextView) findViewById(com.weeknday.onething.R.id.status);

            SetBtnClickListener();
        }
        else
            SetLayoutVisibility();

        SetGoogleSignInConfig();

        if (!m_bIsLogout)
            signIn();
        else
            SetLogout(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        SilentSignIn();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //m_nCurStatus = HirooTypes.STATUS_LOGIN_GOOGLE_ONDESTROY;
        //mHandler.sendEmptyMessage(m_nCurStatus);
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        m_Log.write(AndroidLog.LOGTYPE_INFO, "onActivityResult : ");

        m_Log.write(AndroidLog.LOGTYPE_INFO, "          Request Code : %d", requestCode);
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          Result Code : %d", resultCode);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private static void handleSignInResult(GoogleSignInResult result) {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "handleSignInResult : ");

        boolean m_bAuthResult = result.isSuccess();

        m_nAuthState = result.getStatus().getStatusCode();
        m_strAuthStateMsg = result.getStatus().getStatusMessage();

        m_Log.write(AndroidLog.LOGTYPE_INFO, "          Status Code : %d", m_nAuthState);
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          Status Msg : %s", m_strAuthStateMsg);

        if (m_bAuthResult) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            m_strID = acct.getId();
            m_strIdToken = acct.getIdToken();
            m_strEmail = acct.getEmail();
            m_strDisplayName = acct.getDisplayName();
            String m_strServerAuthCode = acct.getServerAuthCode();

            if (m_bIsInitCtrl)
                mStatusTextView.setText(acct.getDisplayName());

            if (mHandler != null) {
                m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE;
                mHandler.sendEmptyMessage(m_nCurStatus);
            }
        } else {
            // Signed out, show unauthenticated UI.
            if (mHandler != null) {
                if (m_nAuthState == 4) {     // SIGN_IN_REQUIRED
                    m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED;
                } else {
                    m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED;
                }

                mHandler.sendEmptyMessage(m_nCurStatus);
            }
        }

        m_cAct.finish();
    }
    // [END handleSignInResult]

    private void SetGoogleSignInConfig() {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "SetGoogleSignInConfig : ");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("259687128945-ernfud0t817eguikblau57gbcu2fe1bq.apps.googleusercontent.com")  // oauth2:server:client_id:
                .requestEmail()
                .build();



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                //.addScope(new Scope(Scopes.PROFILE))
                .build();

    }


    public static void SilentSignIn() {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {

            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    // [START signIn]
    private void signIn() {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "signIn : ");

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    public static void signOut() {
        mGoogleApiClient.connect();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NotNull Status status) {
                        if (status.isSuccess())
                            m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_LOGOUT_SUCCESS;
                        else
                            m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_LOGOUT_FAILED;

                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(m_nCurStatus);
                        }
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    public static void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NotNull Status status) {
                    }
                });
    }
    // [END revokeAccess]

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "onConnectionFailed : ");

        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        String TAG = "GoogleActivity";
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : %d", connectionResult.getErrorCode());
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : %s", connectionResult.getErrorMessage());
        if (mHandler != null) {
            m_nCurStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED;
            mHandler.sendEmptyMessage(m_nCurStatus);
        }

        m_cAct.finish();
    }

    private static void showProgressDialog() {

    }

    private static void hideProgressDialog() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case com.weeknday.onething.R.id.sign_in_button:
                signIn();
                break;

            case com.weeknday.onething.R.id.sign_out_button:
                signOut();
                break;

            case com.weeknday.onething.R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }

    public static void SetHandler(WebActivity.SendMassgeHandler handler) {
        mHandler = handler;
    }


}
