package com.weeknday.onething;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.webkit.ClientCertRequest;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.Session;
import com.openlibrary.common.contact.ContactAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Calendar;

import uk.co.senab.photoview.PhotoViewAttacher;

public class WebActivity extends Activity implements GestureDetector.OnGestureListener {
    //region  vars
    private static final String TAG = "WebActivity";
    private final String m_strEnvFileName = "cheri.env";
    private final String[] m_strSwipePageArray = {"adopt", "adopt?t=normal", "adopt?t=reservation", "funeral", "freemarket", "freemarket?t=snack", "freemarket?t=goods", "freemarket?t=etc"};
    private static final String TYPE_IMAGE = "image/*";
    private final String m_strLoadUrl = "https://onething.weeknday.com/";
    private final String[] m_strMainPageArray = {"main", "#"};
    private final String m_strAppInfoUrl = m_strLoadUrl + "api/app/version?os=android";
    private String m_strEnvPath = "";
    private String m_strPackageName = "";
    private String m_strTouchUrl = "";
    private String m_strRunLink = "";
    private String m_strCreateUrl = "";
    private String m_strOptions = "";
    private String m_strPkgVersion = "";
    private int m_nPkgLevel = 0;
    private String m_strPkgContents = "";
    private String m_strPkgDownloadUrl = "";
    private String m_strPkgApplyDate = "";
    private String m_strCurPkgVerion = "";
    private final String m_strReturnType = "";
    private String m_strReturnData = "";
    private String m_strContactData = "";
    public String m_strPhoneNum = "";

    private boolean m_bIsProcKakao = false;
    private boolean m_bIsProcGoogle = false;
    private boolean m_bIsProcNaver = false;
    private boolean m_bIsPkgUpdate = false;
    private boolean m_bIsLoading = false;


    private static final int SWIPE_MIN_DISTANCE = 400;
    private static final int SWIPE_MAX_OFF_PATH = 1000;
    private static final int SWIPE_THRESHOLD_VELOCITY = 500;
    private static final int INPUT_FILE_REQUEST_CODE = 1000;
    public final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 0;
    public final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    private final int UTYPE_POSTMSG_KAKAO = 0;
    private final int UTYPE_POSTMSG_NAVER = 1;
    private final int UTYPE_POSTMSG_GOOGLE = 2;
    private final int UTYPE_POSTMSG_INTRO = 10;


    private WebView mWebView = null;
    private WebView mChildWebView = null;
    private ImageView m_ivImage = null;
    private SendMassgeHandler mMainHandler = null;
    private MotionEvent m_cPrevDownME = null;
    private AndroidFile m_File = null;
    private static AndroidLog m_Log = null;
    private AndroidDevice m_cDevice = null;
    private Activity m_cAct = null;
    private KakaoLogin m_cKakao = null;
    private NaverLogin m_cNaver = null;
    private final Bitmap m_UrlImage = null;
    PhotoViewAttacher m_cPVAttacher = null;
    public AlertDialog m_AlertDlg = null;
    public AlertDialog m_ExitDlg = null;
    public AlertDialog m_CallDlg = null;
    static ImageDialog m_cImgDlg = null;
    private LoadingDialog m_cDlgLoading = null;
    private LongPressChecker mLongPressChecker = null;


    private final Handler mhandler = new Handler();
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    public int m_nStatus = OneThingTypes.STATUS_UNKNOWN;


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private GestureDetector gestureScanner;

    //endregion
    //region  overrides
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.weeknday.onething.R.layout.act_web);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mWebView = (WebView) findViewById(com.weeknday.onething.R.id.wvMain);
        m_ivImage = (ImageView) findViewById(com.weeknday.onething.R.id.ivImage);

        m_strCurPkgVerion = GetPackageVersion(); m_cAct = this;
        m_cDevice = new AndroidDevice(WebActivity.this);
        if (shouldUpdateApp()) {
            PackageUpdateDialog cDlgPkgUpdate = new PackageUpdateDialog(this, WebActivity.this);
            cDlgPkgUpdate.setStatus(m_nPkgLevel, m_strPkgDownloadUrl);
            cDlgPkgUpdate.show();
        }
        gestureScanner = new GestureDetector(this, this);
        mMainHandler = new SendMassgeHandler();


        // Allocate AndroidLog Class
        if (m_Log == null) {
            GetWriteFilePermission();
            m_Log = new AndroidLog();
            InitEnvSetting();

            m_Log.SetIsWriteLog(true);
            m_Log.initialize("com.weeknday", "onething.txt", true);
        }

        SetSwipeRefresh();

        new LongPressChecker(this).setOnLongPressListener(new LongPressChecker.OnLongPressListener() {
            @Override
            public void onLongPressed() {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onLongPressed : ");
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (m_cImgDlg != null) {
                            if (m_cImgDlg.IsShowing()) {
                                m_cImgDlg.dismiss();
                                m_cImgDlg = null;
                            }

                            mLongPressChecker.stopTimeout();
                        } else {
                            m_Log.write(AndroidLog.LOGTYPE_INFO, "          Dialog Execute : ");
                            m_cImgDlg = new ImageDialog(m_cAct);
                            m_cImgDlg.show(m_strTouchUrl);
                        }
                    }
                });

            }
        });



        GetAppKeyHash();
        // AlertDialog Initialize
        InitAlertDialog();

        // 메인 핸들러 생성

        setWebViewSetting();
        String strIntro = SendToWebForIntro(FirebaseInstanceId.getInstance().getToken(), m_cDevice.getSdkVersion(), m_cDevice.GetModel(), m_strCurPkgVerion, ReadEnvForAutoLogin(), ReadEnvForRunCount());
        SendMessageToWeb(UTYPE_POSTMSG_INTRO, strIntro);

        WriteEnv("");

    }

    @Override
    protected void onDestroy() {
        if (m_ivImage != null)
            m_ivImage.setImageBitmap(null);

        RecycleUtils.recursiveRecycle(getWindow().getDecorView());
        System.gc();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (m_bIsLoading)
            return;

        if (m_cDlgLoading != null) {
            if (m_cDlgLoading.isShowing())
                m_cDlgLoading.dismiss();

            m_cDlgLoading = null;
        }

        if (m_cPVAttacher != null) {
            m_ivImage.setVisibility(View.GONE);
            m_ivImage.destroyDrawingCache();
            m_cPVAttacher.cleanup();
            m_cPVAttacher = null;

            return;
        }

        WebBackForwardList cBackUrlListMain = null;
        WebBackForwardList cBackUrlListChild = null;

        String strCurUrl = "";

        if (mChildWebView != null)     // 새창
        {
            cBackUrlListChild = mChildWebView.copyBackForwardList();

            String strRootUrl = cBackUrlListChild.getItemAtIndex(0).getUrl();

            strCurUrl = cBackUrlListChild.getCurrentItem().getUrl();

            if (strRootUrl.compareToIgnoreCase(strCurUrl) == 0) {
                mChildWebView.clearHistory();
                mChildWebView.removeView(mChildWebView);    // 화면에서 제거
                mChildWebView.setVisibility(View.GONE);
                mChildWebView.destroy();
                mChildWebView = null;
            } else {
                mChildWebView.goBackOrForward(-1);   // 이전 페이지로 이동.
            }
        } else {
            if (mWebView != null) {
                cBackUrlListMain = mWebView.copyBackForwardList();


                strCurUrl = cBackUrlListMain.getCurrentItem().getUrl();

                boolean bRunExitDlg = false;
                for (String s : m_strMainPageArray) {
                    if (strCurUrl.compareToIgnoreCase(m_strLoadUrl + s) == 0) {
                        bRunExitDlg = true;
                        break;
                    }
                }

                if (bRunExitDlg)
                    m_ExitDlg.show();
                else {
                    if (strCurUrl.compareTo(m_strLoadUrl) == 0)
                        m_ExitDlg.show();
                    else {
                        if (cBackUrlListMain.getCurrentIndex() <= 0 && !mWebView.canGoBack()) {   // 처음 들어온 페이지 이거나, History가 없는 경우
                            m_ExitDlg.show();
                            //super.onBackPressed();
                        } else {

                            int nUrlLength = strCurUrl.length() - 1;
                            String strLastChar = String.valueOf(strCurUrl.charAt(nUrlLength));

                            int nQuestionPos = strCurUrl.indexOf("?");

                            String strSubString = "";
                            if (nQuestionPos > 0)
                                strSubString = strCurUrl.substring(0, nQuestionPos);
                            else
                                strSubString = strCurUrl;

                            //https://pay.billgate.net/credit/smartphone/certify.jsp
                            if ((strSubString.compareToIgnoreCase("https://pay.billgate.net/credit/smartphone/certify.jsp") == 0) ||
                                    (strSubString.compareToIgnoreCase("https://pay.billgate.net/account/smartphone/certify.jsp") == 0))
                                mWebView.goBackOrForward(-5);
                            else if ((strLastChar.compareToIgnoreCase("#") == 0) ||
                                    (strSubString.startsWith("http://xpay.lgdacom.net")) ||
                                    (strSubString.startsWith("https://xpay.lgdacom.net")) ||
                                    (strSubString.compareToIgnoreCase(m_strLoadUrl + "adopt/order") == 0) ||
                                    (strSubString.compareToIgnoreCase(m_strLoadUrl + "adopt/contract") == 0))
                                mWebView.goBackOrForward(-2);
                            else if ((strSubString.compareToIgnoreCase("https://pay.billgate.net/credit/smartphone/auth.jsp") == 0)) {
                                for (int i = 0; i < cBackUrlListMain.getSize(); i++) {
                                    String strUrl = cBackUrlListMain.getItemAtIndex(i).getUrl();

                                    if ((strUrl.compareToIgnoreCase(m_strLoadUrl + "adopt/contract") == 0) ||
                                            (strUrl.compareToIgnoreCase("http://m.hiroo.co.kr/adopt/contract") == 0)) {
                                        mWebView.goBackOrForward(-(cBackUrlListMain.getSize() - i));
                                        break;
                                    }
                                }
                            } else
                                mWebView.goBackOrForward(-1);   // 이전 페이지로 이동.

                            // History 삭제
                            //mWebView.clearHistory();
                        }
                    }
                }
            } else
                m_ExitDlg.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        RunAuthPermission(requestCode, grantResults);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Sleep(1000);
            return;
        }

        if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri[] results = new Uri[]{getResultUri(data)};

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            } else {
                if (mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri result = getResultUri(data);

                Log.d(getClass().getName(), "openFileChooser : " + result);
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        } else {
            if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
            if (mUploadMessage != null) mUploadMessage.onReceiveValue(null);
            mFilePathCallback = null;
            mUploadMessage = null;
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.v(getClass().getName(), "onDown : " + e.getAction() + " : " + e.getDownTime());

        m_cPrevDownME = e;

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.v(getClass().getName(), "onLongPress : " + e.getAction() + " : " + (e.getDownTime() - m_cPrevDownME.getDownTime()));

        if ((e.getDownTime() - m_cPrevDownME.getDownTime()) > 1000)
            ShowImageDialog();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.v(getClass().getName(), "onFling");

        try {
            Log.v(getClass().getName(), "onFling : LeftRight : " + (Math.abs(e1.getX() - e2.getX())));
            Log.v(getClass().getName(), "onFling : UpDown : " + (Math.abs(e1.getY() - e2.getY())));
            Log.v(getClass().getName(), "onFling : VelocityX : " + (Math.abs(velocityX)));
            Log.v(getClass().getName(), "onFling : VelocityY : " + (Math.abs(velocityY)));


            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            else {
                if ((e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) && Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY)
                    Log.v(getClass().getName(), "Swipe down");
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        return true;
    }
    //endregion
    //region functions
    public void InitEnvSetting() {
        if (m_File == null) {
            GetWriteFilePermission();
            m_File = new AndroidFile();
        }

        String strPath;
        strPath = Environment.getExternalStorageDirectory() + "/Android/data/";

        m_strEnvPath = strPath + "kr.co.hiroo/Env/";

        m_File.createDirectory(m_strEnvPath);
        m_File.createFile(m_strEnvPath, m_strEnvFileName);
    }

    public void WriteEnv(String strAutoLoginData) {
        InitEnvSetting();

        String strAutoLoginInfo = "";
        int nRunCountInfo = 1;

        try {
            String strReadEnv = m_File.readFile(m_strEnvPath + m_strEnvFileName);

            if (strReadEnv != null) {
                JSONObject jsonObj = new JSONObject(strReadEnv);

                strAutoLoginInfo = jsonObj.getString("AutoLogin");
                nRunCountInfo = jsonObj.getInt("RunCount");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        JSONObject jsonWrite = new JSONObject();

        try {
            if (strAutoLoginData.isEmpty())
                jsonWrite.put("AutoLogin", strAutoLoginInfo);
            else
                jsonWrite.put("AutoLogin", strAutoLoginData);

            jsonWrite.put("RunCount", nRunCountInfo + 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String strWriteData;
        strWriteData = jsonWrite.toString();

        m_File.writeFile(m_strEnvPath + m_strEnvFileName, false, strWriteData.getBytes());
    }

    public String ReadEnvForAutoLogin() {
        InitEnvSetting();

        String strRes = "";

        try {
            String strReadEnv = m_File.readFile(m_strEnvPath + m_strEnvFileName);

            if (strReadEnv != null) {
                JSONObject jsonObj = new JSONObject(strReadEnv);

                strRes = jsonObj.getString("AutoLogin");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return strRes;
    }

    public int ReadEnvForRunCount() {
        InitEnvSetting();

        int nRes = 0;

        try {
            String strReadEnv = m_File.readFile(m_strEnvPath + m_strEnvFileName);

            if (strReadEnv != null) {
                JSONObject jsonObj = new JSONObject(strReadEnv);

                nRes = jsonObj.getInt("RunCount");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return nRes;
    }

    public String ReadContactInfo() {
        Thread cThread = new Thread() {
            @Override
            public void run() {

                ContactAPI cAPI = new ContactAPI(m_cAct, m_cAct);
                m_strContactData = cAPI.ReadContactListToString();
            }
        };

        cThread.start();

        try {
            cThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return m_strContactData;
    }

    public String SendToWebForIntro(String strDeviceTokenID, String strOSVer, String strDeviceModel, String strAppVersion, String strAutoLogin, int nRunCount) {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("os", "Android");
            jsonWrite.put("token", strDeviceTokenID);
            jsonWrite.put("device", strOSVer);
            jsonWrite.put("model", strDeviceModel);
            jsonWrite.put("appversion", strAppVersion);
            jsonWrite.put("autologin", strAutoLogin);
            jsonWrite.put("runcount", nRunCount);

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return strRes;
    }

    public JSONObject MakeReturnData(String strApi, String strSNSType) {
        JSONObject jsonRes = null;

        if (strApi.compareToIgnoreCase("CheckApp") == 0) jsonRes = ReturnToWebForCheckApp("hiroo", FirebaseInstanceId.getInstance().getToken());
        else if (strApi.compareToIgnoreCase("GetNetworkInfo") == 0) {
            jsonRes = ReturnToWebForGetNetworkInfo();
        }
        else if (strApi.compareToIgnoreCase("ExternSNSAccount") == 0) {
            m_nStatus = OneThingTypes.STATUS_UNKNOWN;
            if (strSNSType.compareToIgnoreCase("kakao") == 0) {
                showProgressDialog();

                m_Log.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : KAKAO");
                //if( !m_bIsProcKakao )
                {
                    m_bIsProcKakao = true;

                    m_Log.write(AndroidLog.LOGTYPE_INFO, "          : START");
                    m_cKakao = new KakaoLogin(mMainHandler, WebActivity.this);
                    m_cKakao.KakaoLoginStart();
                }
            }
            else if (strSNSType.compareToIgnoreCase("google") == 0) {
                showProgressDialog();

                m_Log.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : GOOGLE");
                { m_bIsProcGoogle = true;

                    m_Log.write(AndroidLog.LOGTYPE_INFO, "          : START");
                    GoogleActivity.SetHandler(mMainHandler);
                    Intent i = new Intent(WebActivity.this, GoogleActivity.class);
                    startActivity(i);

                    int nTimeout = 0;
                    while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                        if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED) {
                            GoogleActivity.SilentSignIn();
                        }

                        if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                                m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED)
                            break;

                        Sleep(1000);

                        if (nTimeout > 10)
                            break;

                        nTimeout++;
                    }

                    m_Log.write(AndroidLog.LOGTYPE_INFO, "          : END");

                    hideProgressDialog();

                }
            }
            else if (strSNSType.compareToIgnoreCase("naver") == 0) {
                if (!m_bIsProcNaver) {
                    m_bIsProcNaver = true;
                    m_cNaver = new NaverLogin(mMainHandler, WebActivity.this);
                }
            }

            //jsonRes = ReturnToWebForExternSNSAccount(strSNSType);
        }

        else if (strApi.compareToIgnoreCase("MemberWithdraw") == 0) {
            int nStatus = 0;

            if (strSNSType.compareToIgnoreCase("kakao") == 0) {
                m_cKakao.Logout();
                nStatus = 0;//m_cKakao.GetLastStatus();
            } else if (strSNSType.compareToIgnoreCase("google") == 0) {
                GoogleActivity.SetHandler(mMainHandler);
                GoogleActivity.SetLogout(true);
                Intent i = new Intent(WebActivity.this, GoogleActivity.class);
                startActivity(i);

                int nTimeout = 0;
                while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                    //GoogleActivity.SilentSignIn();

                    if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                            m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED)
                        break;

                    Sleep(1000);

                    if (nTimeout > 10)
                        break;

                    nTimeout++;
                }

                //GoogleActivity.revokeAccess();
                GoogleActivity.signOut();
                nStatus = 0;//GoogleActivity.GetLastStatus();
            } else if (strSNSType.compareToIgnoreCase("naver") == 0) {
                m_cNaver.Logout();
                nStatus = 0;//m_cNaver.GetLastStatus();
            }

            jsonRes = ReturnToWebForMemberWithdraw(strSNSType, nStatus);
        }

        return jsonRes;
    }

    public String ReturnDataToWeb(String strApi, String strSNSType) {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("Api", strApi);

            if (strApi.compareToIgnoreCase("GetContactInfo") == 0) {
                String strContactData = ReadContactInfo();
                jsonWrite.put("Data", strContactData);
            } else {
                JSONObject jsonData = MakeReturnData(strApi, strSNSType);
                jsonWrite.put("Data", jsonData);
            }

            jsonWrite.put("Result", "success");

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return strRes;
    }

    public JSONObject ReturnToWebForCheckApp(String strApp, String strTokenID) {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("App", strApp);
            //jsonWrite.put( "TokenID", strTokenID );

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonWrite;
    }

    public JSONObject ReturnToWebForGetNetworkInfo() {
        String strRes = "";

        AndroidNetwork cAndroidNet = new AndroidNetwork(this);
        cAndroidNet.GetNetworkConnectInfo();

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("DeviceNetType", cAndroidNet.GetDeviceNetworkType());
            jsonWrite.put("NetworkClassType", cAndroidNet.GetNetworkClassType());
            jsonWrite.put("NetConnType", cAndroidNet.GetCurrentConnectNetworkTypeName());
            jsonWrite.put("IsConnect", cAndroidNet.IsConnect());
            jsonWrite.put("IsAvailable", cAndroidNet.IsAvailable());
            jsonWrite.put("IsRoaming", cAndroidNet.IsRoaming());

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        cAndroidNet.InitSetCall();

        return jsonWrite;
    }

    public JSONObject ReturnToWebForMemberWithdraw(String strAccountType, int nStatus) {
        String strRes = "";

        JSONObject jsonWrite = new JSONObject();

        try {
            jsonWrite.put("AccInfo", strAccountType);
            jsonWrite.put("Status", nStatus);

            strRes = jsonWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonWrite;
    }

    public boolean shouldUpdateApp() {
        boolean bRes = false;

        Thread cThread = new Thread() {
            @Override
            public void run() {
                String strData = AndroidHttpUtil.DownloadData(m_strAppInfoUrl);

                if (AndroidHttpUtil.m_nLastError == 0) {
                    try {
                        if (strData != null) {
                            JSONObject jsonObj = new JSONObject(strData);

                            m_strPkgVersion = jsonObj.getString("version");

                            if (m_strPkgVersion.compareToIgnoreCase(m_strCurPkgVerion) > 0) {
                                m_nPkgLevel = jsonObj.getInt("level");
                                m_strPkgContents = jsonObj.getString("contents");
                                m_strPkgDownloadUrl = jsonObj.getString("download_url");
                                m_strPkgApplyDate = jsonObj.getString("apply_date");

                                m_bIsPkgUpdate = true;
                            }
                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };

        cThread.start();

        try {
            cThread.join();

            bRes = m_bIsPkgUpdate;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return bRes;

    }

    private void showProgressDialog() {
        //m_cCtx = cCtx;

        mhandler.post(new Runnable() {
            @Override
            public void run() {
                m_bIsLoading = true;

                if (m_cDlgLoading == null) {
                    m_cDlgLoading = new LoadingDialog(m_cAct);
                    m_cDlgLoading.show();
                }
            }
        });
    }

    private void hideProgressDialog() {
        mhandler.post(new Runnable() {
            @Override
            public void run() {
                m_bIsLoading = false;

                if (m_cDlgLoading != null && m_cDlgLoading.isShowing()) {
                    m_cDlgLoading.dismiss();
                    m_cDlgLoading = null;
                }
            }
        });
    }

    private String GetPackageVersion() {
        String strVersion = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

            strVersion = info.versionName;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }

        return strVersion;
    }

    private void GetAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());

                StringBuilder strSha1 = new StringBuilder();
                for (int i = 0; i < md.digest().length; i++) {
                    strSha1.append(String.valueOf(md.digest()[i]));

                    if (i != (md.digest().length - 1))
                        strSha1.append(":");
                }

                String something = new String(Base64.encode(md.digest(), 0));


                Log.e(TAG, "GetAppKeyHash: " + something);
                Log.e(TAG, "GetAppKeyHash: " + strSha1.toString());

                m_Log.write(AndroidLog.LOGTYPE_INFO, "GetAppKeyHash : %s", something);
                m_Log.write(AndroidLog.LOGTYPE_INFO, "GetAppKeyHash : %s", strSha1.toString());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }
    }

    public void ShowImageDialog() {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          Dialog Execute : ");
        m_cImgDlg = new ImageDialog(m_cAct);
        m_cImgDlg.show(m_strTouchUrl);
    }

    public boolean IsSwipeActionPage(String strUrl) {
        boolean bRes = false;

        int nArrSize = m_strSwipePageArray.length;
        for (String s : m_strSwipePageArray) {
            if (strUrl.compareToIgnoreCase(m_strLoadUrl + s) == 0) {
                bRes = true;
                break;
            }
        }

        return bRes;
    }

    public void SetSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(com.weeknday.onething.R.id.swipeRefresh);
        //색상지정
        mSwipeRefreshLayout.setColorSchemeColors(Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                // 0.1초 후에 페이지를 리로딩하고 동글뱅이를 닫아준다.setRefreshing(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.reload();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 1);      // 1/1000초
            }
        });

        mSwipeRefreshLayout.setEnabled(false);
    }


    public void RunAuthPermission(int nRequestCode, int[] grantResults) {
        switch (nRequestCode) {
            case MY_PERMISSIONS_REQUEST_CALL_PHONE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허가
                    // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                    SendPhoneCall(m_strPhoneNum);
                }  // 권한 거부
                // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다


                break;

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:


                break;

            case MY_PERMISSIONS_REQUEST_CAMERA:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허가
                    // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                    GetCameraPermission();
                }

                break;

        }
    }

    public void SendMessageToWeb(int nType, String strMsg) {
        m_Log.write(AndroidLog.LOGTYPE_INFO, "SendMessageToWeb");

        String strURL = "";

        switch (nType) {
            case UTYPE_POSTMSG_KAKAO:
                //strURL = m_strLoadUrl + "login/mobile_kakao";
                //break;
            case UTYPE_POSTMSG_NAVER:
                //strURL = m_strLoadUrl + "login/mobile_naver";
                //break;
            case UTYPE_POSTMSG_GOOGLE:
                //strURL = m_strLoadUrl + "login/mobile_naver";
                strURL = m_strLoadUrl + "login/sns";
                break;
            case UTYPE_POSTMSG_INTRO:
                strURL = m_strLoadUrl + "start";
                break;
        }

        strMsg = "data=" + strMsg;

        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : SendURL - %s", strURL);
        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : SendMsg - %s", strMsg);
        mWebView.postUrl(strURL, strMsg.getBytes());
    }

    private Uri getResultUri(Intent data) {
        Uri result = null;
        if (data == null || TextUtils.isEmpty(data.getDataString())) {
            // If there is not data, then we may have taken a photo
            if (mCameraPhotoPath != null)
                result = Uri.parse(mCameraPhotoPath);
        } else {
            String filePath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                filePath = data.getDataString();
            else
                filePath = "file:" + RealPathUtil.getRealPath(this, data.getData());

            result = Uri.parse(filePath);
        }

        return result;
    }

    private void InitAlertDialog() {
        m_AlertDlg = new AlertDialog.Builder(this).create();
        m_AlertDlg.setButton(
                DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        m_ExitDlg = new AlertDialog.Builder(this).create();

        m_ExitDlg.setMessage("종료 하시겠습니까?");
        m_ExitDlg.setButton(DialogInterface.BUTTON_POSITIVE, "예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                Thread cThread = new Thread() {
                    @Override
                    public void run() {
                        String strData = AndroidHttpUtil.DownloadData(m_strLoadUrl + "login/bye");


                    }
                };

                cThread.start();

                try {
                    cThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (m_cAct != null)
                    m_cAct.finish();
                else
                    System.exit(0);

            }
        });
        m_ExitDlg.setButton(
                DialogInterface.BUTTON_NEGATIVE, "아니오", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // History 삭제
                        mWebView.clearHistory();

                    }
                });

        m_CallDlg = new AlertDialog.Builder(this).create();
        m_CallDlg.setButton(
                DialogInterface.BUTTON_POSITIVE, "통화", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SendPhoneCall(m_strPhoneNum);
                    }
                });
        m_CallDlg.setButton(
                DialogInterface.BUTTON_NEGATIVE, "취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
    }

    public void setWebViewSetting() {
        Intent intent = getIntent();
        String param = intent.getDataString();

        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setSupportMultipleWindows(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);      // 동영상 자동 재생

        mWebView.getSettings().setSaveFormData(true);


        mWebView.clearCache(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.addJavascriptInterface(new AndroidBridge(), "hirooapp");

        mWebView.setWebChromeClient(new OneThingWebChromeClient());
        mWebView.setWebViewClient(new OneThingWebViewClient());
        mWebView.setWebViewClient(new MyWebViewClient());


        String strUrl = "";
        if (param == null || param.equals(""))
            strUrl = m_strLoadUrl;
        else
            strUrl = param.substring(param.indexOf("hiroo"));

        mWebView.loadUrl(strUrl);


        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                GetWriteFilePermission();
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file");
                    String fileName = contentDisposition.replace("inline; filename=", "");
                    fileName = fileName.replaceAll("\"", "");
                    request.setTitle(fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                Log.v(getClass().getName(), "onTouch : " + event.getAction());
                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                gestureScanner.onTouchEvent(event);

                Log.e(TAG, "onTouch: " + event);




                if (result != null) {
                    Log.e(TAG, "onTouch: " + result.getExtra() );

                    if (result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
                        m_strTouchUrl = result.getExtra();

                        Log.v(getClass().getName(), "onTouch : WebView.HitTestResult.IMAGE_TYPE ");
                    }
                }

                return false;
            }
        });
    }

    public void GetCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }

            return;
        }
    }

    public void GetWriteFilePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }

            return;
        }
    }

    public void SendPhoneCall(String strPhoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }

            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + strPhoneNumber));
        m_cAct.startActivity(intent);
    }

    public void Sleep(int nTime) {
        try {
            Thread.sleep(nTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //endregion
    //region  unused overrides
    @Override
    public void onShowPress(MotionEvent e) {
        Log.v(getClass().getName(), "onShowPress : " + e.getAction());
    }
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.v(getClass().getName(), "onSingleTapUp : " + e.getAction());
        return false;
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.v(getClass().getName(), "onScroll : ");
        return false;
    }

    //endregion
    //region  extra classes
    public static class LongPressChecker {
        public interface OnLongPressListener {
            void onLongPressed();
        }

        private final Handler mHandler = new Handler();
        private final LongPressCheckRunnable mLongPressCheckRunnable = new LongPressCheckRunnable();

        private View mTargetView;
        private OnLongPressListener mOnLongPressListener;
        private boolean mLongPressed = false;

        public LongPressChecker(Context context) {
            if (Looper.myLooper() != Looper.getMainLooper())
                throw new RuntimeException();

            int mLongPressTimeout = ViewConfiguration.getLongPressTimeout() + 1500;        // 1초 : ViewConfiguration.getLongPressTimeout() = 500 milliseconds.

            m_Log.write(AndroidLog.LOGTYPE_INFO, "LongPressTimeout : %s", String.valueOf(mLongPressTimeout));
        }

        public void setOnLongPressListener(OnLongPressListener listener) {
            mOnLongPressListener = listener;
        }

        public void stopTimeout() {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "Stop Timeout : ");
            Log.v(getClass().getName(), "Stop Timeout : ");

            if (!mLongPressed)
                mHandler.removeCallbacks(mLongPressCheckRunnable);

        }
        private class LongPressCheckRunnable implements Runnable {
            @Override
            public void run() {
                if (m_cImgDlg != null) {
                    if (m_cImgDlg.IsShowing()) {
                        if (m_cImgDlg.IsDismiss()) {
                            m_cImgDlg.dismiss();
                            m_cImgDlg = null;
                        }

                        stopTimeout();
                        return;
                    }
                }

                if (!mLongPressed) {
                    mLongPressed = true;
                    if (mOnLongPressListener != null) {
                        Log.v(getClass().getName(), "mOnLongPressListener != null : ");
                        mTargetView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        mOnLongPressListener.onLongPressed();

                    }
                }
            }
        }
    }

    private class MyWebViewClient extends WebViewClient {
        public static final String INTENT_PROTOCOL_START = "intent:";
        public static final String INTENT_PROTOCOL_INTENT = "#Intent;";
        public static final String INTENT_PROTOCOL_END = ";end;";
        public static final String GOOGLE_PLAY_STORE_PREFIX = "market://details?id=";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("kakaolink", url);
            if (url.startsWith(INTENT_PROTOCOL_START)) {
                final int customUrlStartIndex = INTENT_PROTOCOL_START.length();
                final int customUrlEndIndex = url.indexOf(INTENT_PROTOCOL_INTENT);

                if (customUrlEndIndex < 0) {
                    return false;
                } else {
                    final String customUrl = url.substring(customUrlStartIndex, customUrlEndIndex);
                    try {
                        getBaseContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(customUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (ActivityNotFoundException e) {
                        Log.d("kakaolink", "error");
                        final int packageStartIndex = customUrlEndIndex + INTENT_PROTOCOL_INTENT.length();
                        final int packageEndIndex = url.indexOf(INTENT_PROTOCOL_END);

                        final String packageName = url.substring(packageStartIndex, packageEndIndex < 0 ? url.length() : packageEndIndex);

                        Intent cIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_STORE_PREFIX + packageName));
                        cIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getBaseContext().startActivity(cIntent);
                        //getBaseContext().startActivity(new Intent(Intent.ACTION_VIEW,  Uri.parse(GOOGLE_PLAY_STORE_PREFIX + packageName)));
                    }
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private class OneThingWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //return super.shouldOverrideUrlLoading(view, url);

            m_Log.write(AndroidLog.LOGTYPE_INFO, "shouldOverrideUrlLoading = %s", url);

            if (url.startsWith("mailto:")) {
                //mailto:ironnip@test.com
            } else if (url.startsWith("sms:")) {

            } else if (url.startsWith("tel:")) {

            } else if (url.startsWith("ispmobile:")) {
                m_strPackageName = "kvp.jjy.MispAndroid320";

                if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                    String strDownUrl = "https://play.google.com/store/apps/details?id=" + m_strPackageName;

                    Intent cIntent = new Intent();
                    cIntent.setAction(Intent.ACTION_VIEW);
                    cIntent.setData(Uri.parse(strDownUrl));
                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);

                    onBackPressed();
                } else {
                    Intent cIntent = new Intent();
                    cIntent.setAction(Intent.ACTION_VIEW);
                    cIntent.setData(Uri.parse(url));
                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                }
            } else if (url.startsWith("intent:")) {
                String[] strIntentData = url.split(";");

                try {
                    Intent cRunIntent = null;

                    try {
                        cRunIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    } catch (URISyntaxException ex) {

                    }

                    for (int i = 0; i < strIntentData.length; i++) {
                        if (strIntentData[i].startsWith("package=")) {
                            try {
                                Intent cIntent = new Intent();
                                cIntent.setAction(Intent.ACTION_VIEW);

                                String[] strPackageName = strIntentData[i].split("=");
                                String strPackageDownUrl = "https://play.google.com/store/apps/details?id=" + strPackageName[1];

                                m_strPackageName = strPackageName[1];

                                if (isPackageInstalled(m_strPackageName, m_cAct))
                                    break;
                                else {
                                    cIntent.setData(Uri.parse(strPackageDownUrl));
                                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                                }
                            } catch (ActivityNotFoundException ae) {
                            }
                        }
                    }

                    if (isPackageInstalled(m_strPackageName, m_cAct)) {
                        Uri uri = Uri.parse(cRunIntent.getDataString());
                        cRunIntent = new Intent(Intent.ACTION_VIEW, uri);

                        m_cAct.startActivityForResult(cRunIntent, Activity.RESULT_OK);
                    }
                } catch (ActivityNotFoundException ae) {
                }
            } else {
                String strCreateUrl = view.getUrl();

                if (strCreateUrl.startsWith("http://tpay.billgate.net") ||
                        strCreateUrl.startsWith("https://pay.billgate.net")) {
                    if (url.startsWith("lguthepay-xpay:")) {
                        m_strPackageName = "com.lguplus.paynow";

                        if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                            String strDownUrl = "https://play.google.com/store/apps/details?id=" + m_strPackageName;

                            Intent cIntent = new Intent();
                            cIntent.setAction(Intent.ACTION_VIEW);
                            cIntent.setData(Uri.parse(strDownUrl));
                            m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                        } else {
                            Intent cIntent = new Intent();
                            cIntent.setAction(Intent.ACTION_VIEW);
                            cIntent.setData(Uri.parse(url));
                            m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                        }
                    } else if (url.startsWith("http:") ||
                            url.startsWith("https:")) {
                        view.loadUrl(url);
                    } else if (url.startsWith("market:")) {   //market://details?id=kvp.jjy.MispAndroid320
                        String[] strIntentData = url.split("'?");
                        for (int i = 0; i < strIntentData.length; i++) {
                            if (strIntentData[i].startsWith("id=")) {
                                String[] strPackageName = strIntentData[i].split("=");
                                m_strPackageName = strPackageName[1];
                            }
                        }

                        Intent cIntent = new Intent();
                        cIntent.setAction(Intent.ACTION_VIEW);

                        url = url.replaceFirst("market://", "https://play.google.com/store/apps/");
                        cIntent.setData(Uri.parse(url));
                        m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                    } else if (url.startsWith("intent:")) {
                        String[] strIntentData = url.split(";");

                        try {
                            Intent cRunIntent = null;

                            try {
                                cRunIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            } catch (URISyntaxException ex) {

                            }

                            for (int i = 0; i < strIntentData.length; i++) {
                                if (strIntentData[i].startsWith("package=")) {
                                    try {
                                        Intent cIntent = new Intent();
                                        cIntent.setAction(Intent.ACTION_VIEW);

                                        String[] strPackageName = strIntentData[i].split("=");
                                        String strPackageDownUrl = "https://play.google.com/store/apps/details?id=" + strPackageName[1];

                                        m_strPackageName = strPackageName[1];

                                        if (isPackageInstalled(m_strPackageName, m_cAct))
                                            break;
                                        else {
                                            cIntent.setData(Uri.parse(strPackageDownUrl));
                                            m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                                        }
                                    } catch (ActivityNotFoundException ae) {
                                    }
                                }
                            }

                            if (isPackageInstalled(m_strPackageName, m_cAct)) {
                                Uri uri = Uri.parse(cRunIntent.getDataString());
                                cRunIntent = new Intent(Intent.ACTION_VIEW, uri);

                                m_cAct.startActivityForResult(cRunIntent, Activity.RESULT_OK);
                            }
                        } catch (ActivityNotFoundException ae) {
                        }
                    }

                    return true;
                } else if (url.startsWith("market:")) {
                    //market://details?id=kvp.jjy.MispAndroid320
                    String[] strIntentData = url.split("'?");
                    for (int i = 0; i < strIntentData.length; i++) {
                        if (strIntentData[i].startsWith("id=")) {
                            String[] strPackageName = strIntentData[i].split("=");
                            m_strPackageName = strPackageName[1];
                        }
                    }

                    Intent cIntent = new Intent();
                    cIntent.setAction(Intent.ACTION_VIEW);

                    url = url.replaceFirst("market://", "https://play.google.com/store/apps/");
                    cIntent.setData(Uri.parse(url));
                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                } else if (url.startsWith("lguthepay-xpay:")) {
                    m_strPackageName = "com.lguplus.paynow";

                    Intent cIntent = new Intent();
                    cIntent.setAction(Intent.ACTION_VIEW);

                    if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                        String strDownUrl = "https://play.google.com/store/apps/details?id=" + m_strPackageName;
                        cIntent.setData(Uri.parse(strDownUrl));
                    } else {
                        cIntent.setData(Uri.parse(url));
                    }

                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                } else {
                    view.loadUrl(url);
                }
            }

            return true;
        }

        private boolean isPackageInstalled(String packagename, Context context) {
            PackageManager pm = context.getPackageManager();
            try {
                pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onPageStarted = %s", url);

            if (IsSwipeActionPage(url))
                mSwipeRefreshLayout.setEnabled(true);
            else
                mSwipeRefreshLayout.setEnabled(false);

            m_bIsLoading = true;
            if (m_cDlgLoading == null) {
                m_cDlgLoading = new LoadingDialog(m_cAct);
                m_cDlgLoading.show();

                m_cDlgLoading.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onBackPressed();
                    }
                });
            }
            /*
            if (mProgressDialog == null)
            {
                mProgressDialog = new ProgressDialog(m_cAct);
                mProgressDialog.setMessage("Loading...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCanceledOnTouchOutside(false);

                mProgressDialog.show();

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        onBackPressed();
                    }
                });
            }
            */

            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onPageFinished = %s", url);

            m_bIsLoading = false;

            if (m_cDlgLoading != null) {
                if (m_cDlgLoading.isShowing()) {
                    m_cDlgLoading.dismiss();
                    m_cDlgLoading = null;
                }
            }
            /*
            if( mProgressDialog != null )
            {
                if (mProgressDialog.isShowing())
                {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
            */

            if (url.endsWith(".mp4")) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(url);
                i.setDataAndType(uri, "video/mp4");
                startActivity(i);
            }

            //String strMsg = "안드로이드로 부터 온 메시지";
            //mWebView.loadUrl("javascript:getmessage('"+ strMsg +"')");

            //view.loadUrl("javascript:window.hirooapp.getHtml(document.getElementsByTagName('html')[0].innerHTML);"); //<html></html> 사이에 있는 모든 html을 넘겨준다.

            super.onPageFinished(view, url);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onFormResubmission = %s", view.getUrl());
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            //m_Log.write(AndroidLog.LOGTYPE_INFO, "onLoadResource = %s", url);
        }

        @Override
        public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedClientCertRequest = %s", view.getUrl());
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedHttpError = %s", view.getUrl());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedHttpError = %s", String.valueOf(request.getUrl()));
                m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedHttpError = %d", errorResponse.getStatusCode());
            }
        }

        @Override
        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onTooManyRedirects = %s", view.getUrl());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", view.getUrl());
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %d", errorCode);
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", description);
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", failingUrl);

            if (m_cDlgLoading != null) {
                if (m_cDlgLoading.isShowing()) {
                    m_cDlgLoading.dismiss();
                    m_cDlgLoading = null;
                }
            }
            /*
            if( mProgressDialog != null )
            {
                if (mProgressDialog.isShowing())
                {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
            */

            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            m_Log.write(AndroidLog.LOGTYPE_INFO, "onScaleChanged = %s", view.getUrl());

            super.onScaleChanged(view, oldScale, newScale);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(m_cAct);
            builder.setMessage("SSL 인증서가 올바르지 않습니다. 계속 진행하시겠습니까?");
            builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.proceed();
                }
            });
            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();

            /*
            handler.proceed();
            super.onReceivedSslError(view, handler, error);
            */
        }
    }

    public class OneThingWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            view.removeAllViews();
            //m_bCreateWindow = true;

            m_Log.write(AndroidLog.LOGTYPE_INFO, "onCreateWindow = %s", view.getUrl());

            String strCreateUrl = view.getUrl();


            mChildWebView = new WebView(view.getContext());

            mChildWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            mChildWebView.getSettings().setAllowFileAccessFromFileURLs(true);

            mChildWebView.getSettings().setDomStorageEnabled(true);
            mChildWebView.getSettings().setJavaScriptEnabled(true);
            mChildWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mChildWebView.getSettings().setSupportMultipleWindows(true);
            mChildWebView.getSettings().setAllowFileAccess(true);
            mChildWebView.getSettings().setAllowContentAccess(true);
            mChildWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
            mChildWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            mChildWebView.getSettings().setSaveFormData(true);

            //mChildWebView.getSettings().setSupportZoom(true);
            //mChildWebView.getSettings().setBuiltInZoomControls(true);

            mChildWebView.clearCache(false);
            mChildWebView.getSettings().setAppCacheEnabled(true);
            mChildWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

            mChildWebView.getSettings().setUseWideViewPort(true);
            mChildWebView.getSettings().setLoadWithOverviewMode(true);

            mChildWebView.setWebChromeClient(new OneThingWebChromeClient());
            mChildWebView.setWebViewClient(new OneThingWebViewClient());
            mChildWebView.setWebViewClient(new MyWebViewClient());
            mChildWebView.addJavascriptInterface(new AndroidBridge(), "hirooapp");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                mChildWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            else
                mChildWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            mChildWebView.setHorizontalScrollBarEnabled(false);
            mChildWebView.setVerticalScrollBarEnabled(false);
            //mChildWebView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            LinearLayout.LayoutParams lyWebViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            mChildWebView.setLayoutParams(lyWebViewParams);

            view.addView(mChildWebView);

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mChildWebView);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView view) {
            super.onCloseWindow(view);
            view.removeView(view);    // 화면에서 제거
            view.setVisibility(View.GONE);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            //return super.onJsAlert(view, url, message, result);
            m_AlertDlg.setMessage(message);
            m_AlertDlg.show();

            result.confirm();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            // TODO Auto-generated method stub
            //return super.onJsConfirm(view, url, message, result);
            m_AlertDlg.setMessage("Confirm : " + message);
            m_AlertDlg.show();

            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
        }

        // For Android Version < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(TYPE_IMAGE);
            startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);
        }

        // For 3.0 <= Android Version < 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
            openFileChooser(uploadMsg, acceptType, "");
        }

        // For 4.1 <= Android Version < 5.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
            mUploadMessage = uploadFile;
            imageChooser();
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            System.out.println("WebActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
            if (mFilePathCallback != null)
                mFilePathCallback.onReceiveValue(null);

            mFilePathCallback = filePathCallback;
            //fileChooser();
            imageChooser();
            return true;
        }

        private void fileChooser() {
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType(TYPE_IMAGE);

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        }

        private void imageChooser() {
            GetCameraPermission();

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(getClass().getName(), "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            //contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            contentSelectionIntent.setType(TYPE_IMAGE);

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "이미지 선택");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        }

        private File createImageFile() throws IOException {
            // Create an image file name
            Calendar calendar = Calendar.getInstance();
            String timeStamp = String.format("%04d-%02d-%02d_%02d:%02d:%02d", calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            return imageFile;
        }
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void getHtml(String html) { //위 자바스크립트가 호출되면 여기로 html이 반환됨
            System.out.println(html);
        }

        @JavascriptInterface
        public void callAndroid(final String arg) {
            ReceiveToWebRequest(arg);
        }

        @JavascriptInterface
        public String callAndroid(final String arg, final String arga) {
            return SetResultData(arg, arga, "");
        }

        @JavascriptInterface
        public String callAndroid(final String arg, final String arga, final String argb) {
            return SetResultData(arg, arga, argb);
        }

        @JavascriptInterface
        public void ReturnToWeb(String strReturnData) {
            m_strReturnData = strReturnData;

            mhandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:ReturnMobile('" + m_strReturnData + "')");
                }
            });
        }

        @JavascriptInterface
        public void SharedUrl(String strUrl, String strTitle) {
            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, strTitle);
            intent.putExtra(Intent.EXTRA_TEXT, strUrl);

            // Title of intent
            Intent iShare = Intent.createChooser(intent, "공유하기");
            startActivity(iShare);
        }

        public void ReceiveToWebRequest(String strRequestData) {
            try {
                if (strRequestData != null) {
                    JSONObject jsonObj = new JSONObject(strRequestData);

                    String strApi = "";
                    String strData = "";
                    String strOption = "";

                    strApi = jsonObj.getString("Api");
                    strData = jsonObj.getString("Data");
                    strOption = jsonObj.getString("Option");

                    if (strApi.compareToIgnoreCase("CheckApp") == 0 ||
                            strApi.compareToIgnoreCase("GetGPSInfo") == 0 ||
                            strApi.compareToIgnoreCase("ExternSNSAccount") == 0 ||
                            strApi.compareToIgnoreCase("GetNetworkInfo") == 0 ||
                            strApi.compareToIgnoreCase("MemberWithdraw") == 0 ||
                            strApi.compareToIgnoreCase("GetContactInfo") == 0) {
                        String strRes = "";

                        strRes = ReturnDataToWeb(strApi, strData);

                        ReturnToWeb(strRes);
                    } else {
                        if (strApi.compareToIgnoreCase("SaveAutoLogin") == 0)
                            WriteEnv(strData);
                        else if (strApi.compareToIgnoreCase("PhoneCall") == 0) {
                            m_strPhoneNum = strData;
                            SendPhoneCall(m_strPhoneNum);
                        } else if (strApi.compareToIgnoreCase("RunLink") == 0) {
                            m_strRunLink = strData;

                            mhandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Intent cIntent = new Intent();
                                    cIntent.setAction(Intent.ACTION_VIEW);
                                    cIntent.setData(Uri.parse(m_strRunLink));
                                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                                }
                            });
                        } else if (strApi.compareToIgnoreCase("CreateWindow") == 0) {
                            m_strCreateUrl = m_strLoadUrl + strData;
                            m_strOptions = strOption;

                            mhandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(CreateWebViewActivity.PARAM_TYPE, CreateWebViewActivity.PARAM_TYPE_GENERAL);
                                    bundle.putString(CreateWebViewActivity.PARAM_URL, m_strCreateUrl);
                                    bundle.putString(CreateWebViewActivity.PARAM_OPTIONS, m_strOptions);

                                    Intent cIntent = new Intent(WebActivity.this, CreateWebViewActivity.class);
                                    cIntent.putExtras(bundle);
                                    startActivity(cIntent);
                                }
                            });
                        }
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public String SetResultData(String strArg, String strArgs, String strArg1) {
            String strRes = "";

            if (strArg.compareToIgnoreCase("CheckApp") == 0)
                strRes = "hiroo," + FirebaseInstanceId.getInstance().getToken();
            else if (strArg.compareToIgnoreCase("GetDeviceInfo") == 0) {
                //모바일토큰, 디바이스정보, 모델정보, 가로사이즈, 세로사이즈
                strRes = FirebaseInstanceId.getInstance().getToken() + "," + m_cDevice.getSdkVersion() + "," + m_cDevice.GetModel() + "," + ReadEnvForAutoLogin();// + ","+m_cDevice.GetWidthPx() + "," + m_cDevice.GetHeightPx();
                SendMessageToWeb(UTYPE_POSTMSG_INTRO, strRes);
            } else if (strArg.compareToIgnoreCase("SaveLoginID") == 0)
                strRes = "True";
            else if (strArg.compareToIgnoreCase("SaveAutoLogin") == 0) {
                WriteEnv(strArgs);

                strRes = "True";
            } else if (strArg.compareToIgnoreCase("PhoneCall") == 0) {
                m_strPhoneNum = strArgs;

                SendPhoneCall(m_strPhoneNum);

                strRes = "True";
            } else if (strArg.compareToIgnoreCase("ExternSNSAccount") == 0) {
                if (strArgs.compareToIgnoreCase("kakao") == 0) {
                    showProgressDialog();

                    m_Log.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : KAKAO");
                    //if( !m_bIsProcKakao )
                    {
                        m_bIsProcKakao = true;

                        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : START");
                        m_cKakao = new KakaoLogin(mMainHandler, WebActivity.this);
                        m_cKakao.KakaoLoginStart();

                        strRes = m_cKakao.ResultData();
                    }
                } else if (strArgs.compareToIgnoreCase("google") == 0) {
                    showProgressDialog();

                    m_Log.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : GOOGLE");
                    //if( !m_bIsProcGoogle )
                    {
                        m_bIsProcGoogle = true;

                        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : START");
                        GoogleActivity.SetHandler(mMainHandler);
                        Intent i = new Intent(WebActivity.this, GoogleActivity.class);
                        startActivity(i);

                        int nTimeout = 0;
                        while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                            if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED) {
                                GoogleActivity.SilentSignIn();
                            }

                            if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                                    m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED)
                                break;

                            Sleep(1000);

                            if (nTimeout > 10)
                                break;

                            nTimeout++;
                        }

                        m_Log.write(AndroidLog.LOGTYPE_INFO, "          : END");

                        hideProgressDialog();
                        strRes = GoogleActivity.ResultData();
                        m_bIsProcGoogle = false;

                    }
                } else if (strArgs.compareToIgnoreCase("naver") == 0) {
                    if (!m_bIsProcNaver) {
                        m_bIsProcNaver = true;
                        m_cNaver = new NaverLogin(mMainHandler, WebActivity.this);

                        strRes = m_cNaver.ResultData();
                    }
                } else
                    strRes = "Unknown Interface";
            } else if (strArg.compareToIgnoreCase("RunLink") == 0) {
                m_strRunLink = strArgs;

                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent cIntent = new Intent();
                        cIntent.setAction(Intent.ACTION_VIEW);
                        cIntent.setData(Uri.parse(m_strRunLink));
                        m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                    }
                });
            } else if (strArg.compareToIgnoreCase("CreateWindow") == 0) {
                m_strCreateUrl = m_strLoadUrl + strArgs;
                m_strOptions = strArg1;

                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Bundle bundle = new Bundle();
                        bundle.putInt(CreateWebViewActivity.PARAM_TYPE, CreateWebViewActivity.PARAM_TYPE_GENERAL);
                        bundle.putString(CreateWebViewActivity.PARAM_URL, m_strCreateUrl);
                        bundle.putString(CreateWebViewActivity.PARAM_OPTIONS, m_strOptions);

                        Intent cIntent = new Intent(WebActivity.this, CreateWebViewActivity.class);
                        cIntent.putExtras(bundle);
                        startActivity(cIntent);
                    }
                });
            } else
                strRes = "Unknown Interface,";

            return strRes;
        }
    }

    class SendMassgeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : ");

            hideProgressDialog();

            switch (msg.what) {
                case OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_KAKAO_COMPLETE");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE;

                    SendMessageToWeb(UTYPE_POSTMSG_KAKAO, m_cKakao.ResultDataForJson());
                    m_bIsProcKakao = false;
                    break;

                case OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_KAKAO_SESSIONCLOSED");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED;
                    break;

                case OneThingTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_KAKAO_NOTSIGNEDUP");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP;
                    break;

                case OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_KAKAO_SESSIONOPENFAILED");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED;
                    break;

                case OneThingTypes.STATUS_LOGIN_KAKAO_UNKNOWN:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_KAKAO_UNKNOWN");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_UNKNOWN;
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_COMPLETE");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE;
                    SendMessageToWeb(UTYPE_POSTMSG_GOOGLE, GoogleActivity.ResultDataForJson());
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_SIGNINREQUIRED");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED;
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_ONDESTROY:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_ONDESTROY");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_ONDESTROY;
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_AUTHFAILED");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED;
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_CONNECTIONFAILED");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED;
                    break;

                case OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_GOOGLE_UNKNOWN");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN;
                    break;

                case OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_NAVER_COMPLETE");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE;
                    SendMessageToWeb(UTYPE_POSTMSG_NAVER, m_cNaver.ResultDataForJson());
                    m_bIsProcNaver = false;
                    break;

                case OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_NAVER_USERCANCEL");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL;
                    break;

                case OneThingTypes.STATUS_LOGIN_NAVER_UNKNOWN:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_NAVER_UNKNOWN");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_UNKNOWN;
                    break;

                case OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : STATUS_LOGIN_NAVER_DELETETOKEN");
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN;
                    break;

                default:
                    m_Log.write(AndroidLog.LOGTYPE_INFO, "handleMessage : UNKNOWN(%d)", msg.what);
                    m_nStatus = msg.what;
                    break;
            }
        }
    }
    //endregion
}