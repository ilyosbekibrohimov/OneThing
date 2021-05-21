package com.weeknday.onething

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.webkit.*
import android.webkit.WebView.HitTestResult
import android.webkit.WebView.WebViewTransport
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.iid.FirebaseInstanceId
import com.kakao.auth.Session
import org.json.JSONException
import org.json.JSONObject
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.*

class WebActivity : Activity(), GestureDetector.OnGestureListener {
    private val m_strEnvFileName = "cheri.env"
    private val m_strSwipePageArray = arrayOf(
        "adopt",
        "adopt?t=normal",
        "adopt?t=reservation",
        "funeral",
        "freemarket",
        "freemarket?t=snack",
        "freemarket?t=goods",
        "freemarket?t=etc"
    )
    private val m_strLoadUrl = "https://onething.weeknday.com/"
    private val m_strMainPageArray = arrayOf("main", "#")
    private val m_strAppInfoUrl = m_strLoadUrl + "api/app/version?os=android"
    private var m_strEnvPath = ""
    private var m_strPackageName = ""
    private var m_strTouchUrl: String? = ""
    private var m_strRunLink = ""
    private var m_strCreateUrl = ""
    private var m_strOptions = ""
    private var m_strPkgVersion = ""
    private var m_nPkgLevel = 0
    private var m_strPkgContents = ""
    private var m_strPkgDownloadUrl = ""
    private var m_strPkgApplyDate = ""
    private var m_strCurPkgVerion = ""
    private val m_strReturnType = ""
    private var m_strReturnData = ""
    private val m_strContactData = ""
    var m_strPhoneNum = ""
    private var m_bIsProcKakao = false
    private var m_bIsProcGoogle = false
    private var m_bIsProcNaver = false
    private var m_bIsPkgUpdate = false
    private var m_bIsLoading = false
    val MY_PERMISSIONS_REQUEST_CALL_PHONE = 0
    val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    val MY_PERMISSIONS_REQUEST_CAMERA = 2
    private val UTYPE_POSTMSG_KAKAO = 0
    private val UTYPE_POSTMSG_NAVER = 1
    private val UTYPE_POSTMSG_GOOGLE = 2
    private val UTYPE_POSTMSG_INTRO = 10
    private var mWebView: WebView? = null
    private var mChildWebView: WebView? = null
    private var m_ivImage: ImageView? = null
    private var mMainHandler: SendMassgeHandler? = null
    private var m_cPrevDownME: MotionEvent? = null
    private var m_File: AndroidFile? = null
    private var m_cDevice: AndroidDevice? = null
    private var m_cAct: Activity? = null
    private var m_cKakao: KakaoLogin? = null
    private var m_cNaver: NaverLogin? = null
    private val m_UrlImage: Bitmap? = null
    var m_cPVAttacher: PhotoViewAttacher? = null
    var m_AlertDlg: AlertDialog? = null
    var m_ExitDlg: AlertDialog? = null
    var m_CallDlg: AlertDialog? = null
    private var m_cDlgLoading: LoadingDialog? = null
    private val mLongPressChecker: LongPressChecker? = null
    private val mhandler = Handler()
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    var m_nStatus = OneThingTypes.STATUS_UNKNOWN
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var gestureScanner: GestureDetector? = null

    //endregion
    //region  overrides
    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_web)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mWebView = findViewById<View>(R.id.wvMain) as WebView
        m_ivImage = findViewById<View>(R.id.ivImage) as ImageView
        m_strCurPkgVerion = GetPackageVersion()
        m_cAct = this
        m_cDevice = AndroidDevice(this@WebActivity)
        if (shouldUpdateApp()) {
            val cDlgPkgUpdate = PackageUpdateDialog(this, this@WebActivity)
            cDlgPkgUpdate.setStatus(m_nPkgLevel, m_strPkgDownloadUrl)
            cDlgPkgUpdate.show()
        }
        gestureScanner = GestureDetector(this, this)
        mMainHandler = SendMassgeHandler()


        // Allocate AndroidLog Class
        if (m_Log == null) {
            GetWriteFilePermission()
            m_Log = AndroidLog()
            InitEnvSetting()
            m_Log!!.SetIsWriteLog(true)
            m_Log!!.initialize("com.weeknday", "onething.txt", true)
        }
        SetSwipeRefresh()
        LongPressChecker(this).setOnLongPressListener(object :
            LongPressChecker.OnLongPressListener {
            override fun onLongPressed() {
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onLongPressed : ")
                mhandler.post {
                    if (m_cImgDlg != null) {
                        if (m_cImgDlg!!.IsShowing()) {
                            m_cImgDlg!!.dismiss()
                            m_cImgDlg = null
                        }
                        mLongPressChecker!!.stopTimeout()
                    } else {
                        m_Log!!.write(
                            AndroidLog.LOGTYPE_INFO,
                            "          Dialog Execute : "
                        )
                        m_cImgDlg = ImageDialog(m_cAct as WebActivity)
                        m_cImgDlg!!.show(m_strTouchUrl!!)
                    }
                }
            }
        })
        GetAppKeyHash()
        // AlertDialog Initialize
        initAlertDialog()

        // 메인 핸들러 생성
        setWebViewSetting()
        val strIntro = SendToWebForIntro(
            FirebaseInstanceId.getInstance().token,
            m_cDevice!!.getSdkVersion(),
            m_cDevice!!.getModel(),
            m_strCurPkgVerion,
            ReadEnvForAutoLogin(),
            ReadEnvForRunCount()
        )
        SendMessageToWeb(UTYPE_POSTMSG_INTRO, strIntro)
        WriteEnv("")
    }

    override fun onDestroy() {
        if (m_ivImage != null) m_ivImage!!.setImageBitmap(null)
        RecycleUtils.recursiveRecycle(window.decorView)
        System.gc()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (m_bIsLoading) return
        if (m_cDlgLoading != null) {
            if (m_cDlgLoading!!.isShowing) m_cDlgLoading!!.dismiss()
            m_cDlgLoading = null
        }
        if (m_cPVAttacher != null) {
            m_ivImage!!.visibility = View.GONE
            m_ivImage!!.destroyDrawingCache()
            m_cPVAttacher!!.cleanup()
            m_cPVAttacher = null
            return
        }
        var cBackUrlListMain: WebBackForwardList? = null
        var cBackUrlListChild: WebBackForwardList? = null
        var strCurUrl = ""
        if (mChildWebView != null) // 새창
        {
            cBackUrlListChild = mChildWebView!!.copyBackForwardList()
            val strRootUrl = cBackUrlListChild.getItemAtIndex(0).url
            strCurUrl = cBackUrlListChild.currentItem!!.url
            if (strRootUrl.compareTo(strCurUrl, ignoreCase = true) == 0) {
                mChildWebView!!.clearHistory()
                mChildWebView!!.removeView(mChildWebView) // 화면에서 제거
                mChildWebView!!.visibility = View.GONE
                mChildWebView!!.destroy()
                mChildWebView = null
            } else {
                mChildWebView!!.goBackOrForward(-1) // 이전 페이지로 이동.
            }
        } else {
            if (mWebView != null) {
                cBackUrlListMain = mWebView!!.copyBackForwardList()
                strCurUrl = cBackUrlListMain.currentItem!!.url
                var bRunExitDlg = false
                for (s in m_strMainPageArray) {
                    if (strCurUrl.compareTo(m_strLoadUrl + s, ignoreCase = true) == 0) {
                        bRunExitDlg = true
                        break
                    }
                }
                if (bRunExitDlg) m_ExitDlg!!.show() else {
                    if (strCurUrl.compareTo(m_strLoadUrl) == 0) m_ExitDlg!!.show() else {
                        if (cBackUrlListMain.currentIndex <= 0 && !mWebView!!.canGoBack()) {   // 처음 들어온 페이지 이거나, History가 없는 경우
                            m_ExitDlg!!.show()
                            //super.onBackPressed();
                        } else {
                            val nUrlLength = strCurUrl.length - 1
                            val strLastChar = strCurUrl[nUrlLength].toString()
                            val nQuestionPos = strCurUrl.indexOf("?")
                            var strSubString = ""
                            strSubString =
                                if (nQuestionPos > 0) strCurUrl.substring(
                                    0,
                                    nQuestionPos
                                ) else strCurUrl

                            //https://pay.billgate.net/credit/smartphone/certify.jsp
                            if (strSubString.compareTo(
                                    "https://pay.billgate.net/credit/smartphone/certify.jsp",
                                    ignoreCase = true
                                ) == 0 ||
                                strSubString.compareTo(
                                    "https://pay.billgate.net/account/smartphone/certify.jsp",
                                    ignoreCase = true
                                ) == 0
                            ) mWebView!!.goBackOrForward(-5) else if (strLastChar.compareTo(
                                    "#",
                                    ignoreCase = true
                                ) == 0 ||
                                strSubString.startsWith("http://xpay.lgdacom.net") ||
                                strSubString.startsWith("https://xpay.lgdacom.net") ||
                                strSubString.compareTo(
                                    m_strLoadUrl + "adopt/order",
                                    ignoreCase = true
                                ) == 0 ||
                                strSubString.compareTo(
                                    m_strLoadUrl + "adopt/contract",
                                    ignoreCase = true
                                ) == 0
                            ) mWebView!!.goBackOrForward(-2) else if (strSubString.compareTo(
                                    "https://pay.billgate.net/credit/smartphone/auth.jsp",
                                    ignoreCase = true
                                ) == 0
                            ) {
                                for (i in 0 until cBackUrlListMain.size) {
                                    val strUrl = cBackUrlListMain.getItemAtIndex(i).url
                                    if (strUrl.compareTo(
                                            m_strLoadUrl + "adopt/contract",
                                            ignoreCase = true
                                        ) == 0 ||
                                        strUrl.compareTo(
                                            "http://m.hiroo.co.kr/adopt/contract",
                                            ignoreCase = true
                                        ) == 0
                                    ) {
                                        mWebView!!.goBackOrForward(-(cBackUrlListMain.size - i))
                                        break
                                    }
                                }
                            } else mWebView!!.goBackOrForward(-1) // 이전 페이지로 이동.

                            // History 삭제
                            //mWebView.clearHistory();
                        }
                    }
                }
            } else m_ExitDlg!!.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        RunAuthPermission(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Sleep(1000)
            return
        }
        if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                val results = arrayOf(getResultUri(data))
                onReceiveValue()
                mFilePathCallback = null
            } else {
                if (mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                val result = getResultUri(data)
                Log.d(javaClass.name, "openFileChooser : $result")
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        } else {
            if (mFilePathCallback != null) mFilePathCallback!!.onReceiveValue(null)
            if (mUploadMessage != null) mUploadMessage!!.onReceiveValue(null)
            mFilePathCallback = null
            mUploadMessage = null
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        Log.v(javaClass.name, "onDown : " + e.action + " : " + e.downTime)
        m_cPrevDownME = e
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        Log.v(
            javaClass.name,
            "onLongPress : " + e.action + " : " + (e.downTime - m_cPrevDownME!!.downTime)
        )
        if (e.downTime - m_cPrevDownME!!.downTime > 1000) ShowImageDialog()
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Log.v(javaClass.name, "onFling")
        try {
            Log.v(javaClass.name, "onFling : LeftRight : " + Math.abs(e1.x - e2.x))
            Log.v(javaClass.name, "onFling : UpDown : " + Math.abs(e1.y - e2.y))
            Log.v(javaClass.name, "onFling : VelocityX : " + Math.abs(velocityX))
            Log.v(javaClass.name, "onFling : VelocityY : " + Math.abs(velocityY))
            if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) return false else {
                if (e2.y - e1.y > SWIPE_MIN_DISTANCE && Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY) Log.v(
                    javaClass.name,
                    "Swipe down"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    //endregion
    //region functions
    fun InitEnvSetting() {
        if (m_File == null) {
            GetWriteFilePermission()
            m_File = AndroidFile()
        }
        val strPath: String
        strPath = Environment.getExternalStorageDirectory().toString() + "/Android/data/"
        m_strEnvPath = strPath + "kr.co.hiroo/Env/"
        m_File!!.createDirectory(m_strEnvPath)
        m_File!!.createFile(m_strEnvPath, m_strEnvFileName)
    }

    fun WriteEnv(strAutoLoginData: String) {
        InitEnvSetting()
        var strAutoLoginInfo = ""
        var nRunCountInfo = 1
        try {
            val strReadEnv = m_File!!.readFile(m_strEnvPath + m_strEnvFileName)
            if (strReadEnv != null) {
                val jsonObj = JSONObject(strReadEnv)
                strAutoLoginInfo = jsonObj.getString("AutoLogin")
                nRunCountInfo = jsonObj.getInt("RunCount")
            }
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        val jsonWrite = JSONObject()
        try {
            if (strAutoLoginData.isEmpty()) jsonWrite.put(
                "AutoLogin",
                strAutoLoginInfo
            ) else jsonWrite.put("AutoLogin", strAutoLoginData)
            jsonWrite.put("RunCount", nRunCountInfo + 1)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val strWriteData: String
        strWriteData = jsonWrite.toString()
        m_File!!.writeFile(m_strEnvPath + m_strEnvFileName, false, strWriteData.toByteArray())
    }

    fun ReadEnvForAutoLogin(): String {
        InitEnvSetting()
        var strRes = ""
        try {
            val strReadEnv = m_File!!.readFile(m_strEnvPath + m_strEnvFileName)
            if (strReadEnv != null) {
                val jsonObj = JSONObject(strReadEnv)
                strRes = jsonObj.getString("AutoLogin")
            }
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return strRes
    }

    fun ReadEnvForRunCount(): Int {
        InitEnvSetting()
        var nRes = 0
        try {
            val strReadEnv = m_File!!.readFile(m_strEnvPath + m_strEnvFileName)
            if (strReadEnv != null) {
                val jsonObj = JSONObject(strReadEnv)
                nRes = jsonObj.getInt("RunCount")
            }
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return nRes
    }

    fun SendToWebForIntro(
        strDeviceTokenID: String?,
        strOSVer: String?,
        strDeviceModel: String?,
        strAppVersion: String?,
        strAutoLogin: String?,
        nRunCount: Int
    ): String {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("os", "Android")
            jsonWrite.put("token", strDeviceTokenID)
            jsonWrite.put("device", strOSVer)
            jsonWrite.put("model", strDeviceModel)
            jsonWrite.put("appversion", strAppVersion)
            jsonWrite.put("autologin", strAutoLogin)
            jsonWrite.put("runcount", nRunCount)
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return strRes
    }

    fun MakeReturnData(strApi: String, strSNSType: String): JSONObject? {
        var jsonRes: JSONObject? = null
        if (strApi.compareTo("CheckApp", ignoreCase = true) == 0) jsonRes = ReturnToWebForCheckApp(
            "hiroo",
            FirebaseInstanceId.getInstance().token
        ) else if (strApi.compareTo("GetNetworkInfo", ignoreCase = true) == 0) {
            jsonRes = ReturnToWebForGetNetworkInfo()
        } else if (strApi.compareTo("ExternSNSAccount", ignoreCase = true) == 0) {
            m_nStatus = OneThingTypes.STATUS_UNKNOWN
            if (strSNSType.compareTo("kakao", ignoreCase = true) == 0) {
                showProgressDialog()
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : KAKAO")
                //if( !m_bIsProcKakao )
                run {
                    m_bIsProcKakao = true
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "          : START"
                    )
                    m_cKakao = KakaoLogin(mMainHandler, this@WebActivity)
                    m_cKakao!!.KakaoLoginStart()
                }
            } else if (strSNSType.compareTo("google", ignoreCase = true) == 0) {
                showProgressDialog()
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : GOOGLE")
                run {
                    m_bIsProcGoogle = true
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "          : START"
                    )
                    GoogleActivity.setHandler(mMainHandler)
                    val i = Intent(this@WebActivity, GoogleActivity::class.java)
                    startActivity(i)
                    var nTimeout = 0
                    while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                        if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED) {
                            GoogleActivity.silentSignIn()
                        }
                        if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                            m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED
                        ) break
                        Sleep(1000)
                        if (nTimeout > 10) break
                        nTimeout++
                    }
                    m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          : END")
                    hideProgressDialog()
                }
            } else if (strSNSType.compareTo("naver", ignoreCase = true) == 0) {
                if (!m_bIsProcNaver) {
                    m_bIsProcNaver = true
                    m_cNaver = NaverLogin(mMainHandler, this@WebActivity)
                }
            }

            //jsonRes = ReturnToWebForExternSNSAccount(strSNSType);
        } else if (strApi.compareTo("MemberWithdraw", ignoreCase = true) == 0) {
            var nStatus = 0
            if (strSNSType.compareTo("kakao", ignoreCase = true) == 0) {
                m_cKakao!!.Logout()
                nStatus = 0 //m_cKakao.GetLastStatus();
            } else if (strSNSType.compareTo("google", ignoreCase = true) == 0) {
                GoogleActivity.setHandler(mMainHandler)
                GoogleActivity.SetLogout(true)
                val i = Intent(this@WebActivity, GoogleActivity::class.java)
                startActivity(i)
                var nTimeout = 0
                while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                    //GoogleActivity.SilentSignIn();
                    if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                        m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED
                    ) break
                    Sleep(1000)
                    if (nTimeout > 10) break
                    nTimeout++
                }

                //GoogleActivity.revokeAccess();
                GoogleActivity.signOut()
                nStatus = 0 //GoogleActivity.GetLastStatus();
            } else if (strSNSType.compareTo("naver", ignoreCase = true) == 0) {
                m_cNaver!!.Logout()
                nStatus = 0 //m_cNaver.GetLastStatus();
            }
            jsonRes = ReturnToWebForMemberWithdraw(strSNSType, nStatus)
        }
        return jsonRes
    }

    fun ReturnDataToWeb(strApi: String, strSNSType: String): String {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("Api", strApi)
            val jsonData = MakeReturnData(strApi, strSNSType)
            jsonWrite.put("Data", jsonData)
            jsonWrite.put("Result", "success")
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return strRes
    }

    fun ReturnToWebForCheckApp(strApp: String?, strTokenID: String?): JSONObject {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("App", strApp)
            //jsonWrite.put( "TokenID", strTokenID );
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonWrite
    }

    fun ReturnToWebForGetNetworkInfo(): JSONObject {
        var strRes = ""
        val cAndroidNet = AndroidNetwork(this)
        cAndroidNet.GetNetworkConnectInfo()
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("DeviceNetType", cAndroidNet.GetDeviceNetworkType())
            jsonWrite.put("NetworkClassType", cAndroidNet.GetNetworkClassType())
            jsonWrite.put("NetConnType", cAndroidNet.GetCurrentConnectNetworkTypeName())
            jsonWrite.put("IsConnect", cAndroidNet.IsConnect())
            jsonWrite.put("IsAvailable", cAndroidNet.IsAvailable())
            jsonWrite.put("IsRoaming", cAndroidNet.IsRoaming())
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        cAndroidNet.InitSetCall()
        return jsonWrite
    }

    fun ReturnToWebForMemberWithdraw(strAccountType: String?, nStatus: Int): JSONObject {
        var strRes = ""
        val jsonWrite = JSONObject()
        try {
            jsonWrite.put("AccInfo", strAccountType)
            jsonWrite.put("Status", nStatus)
            strRes = jsonWrite.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonWrite
    }

    fun shouldUpdateApp(): Boolean {
        var bRes = false
        val cThread: Thread = object : Thread() {
            override fun run() {
                val strData = AndroidHttpUtil.downloadData(m_strAppInfoUrl)
                if (AndroidHttpUtil.getStatus() == 0) {
                    try {
                        if (strData != null) {
                            val jsonObj = JSONObject(strData)
                            m_strPkgVersion = jsonObj.getString("version")
                            if (m_strPkgVersion.compareTo(
                                    m_strCurPkgVerion,
                                    ignoreCase = true
                                ) > 0
                            ) {
                                m_nPkgLevel = jsonObj.getInt("level")
                                m_strPkgContents = jsonObj.getString("contents")
                                m_strPkgDownloadUrl = jsonObj.getString("download_url")
                                m_strPkgApplyDate = jsonObj.getString("apply_date")
                                m_bIsPkgUpdate = true
                            }
                        }
                    } catch (e: JSONException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }
        cThread.start()
        try {
            cThread.join()
            bRes = m_bIsPkgUpdate
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return bRes
    }

    private fun showProgressDialog() {
        //m_cCtx = cCtx;
        mhandler.post {
            m_bIsLoading = true
            if (m_cDlgLoading == null) {
                m_cDlgLoading = LoadingDialog(m_cAct!!)
                m_cDlgLoading!!.show()
            }
        }
    }

    private fun hideProgressDialog() {
        mhandler.post {
            m_bIsLoading = false
            if (m_cDlgLoading != null && m_cDlgLoading!!.isShowing) {
                m_cDlgLoading!!.dismiss()
                m_cDlgLoading = null
            }
        }
    }

    private fun GetPackageVersion(): String {
        var strVersion = ""
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            strVersion = info.versionName
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString())
        }
        return strVersion
    }

    private fun GetAppKeyHash() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                var md: MessageDigest
                md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val strSha1 = StringBuilder()
                for (i in md.digest().indices) {
                    strSha1.append(md.digest()[i].toString())
                    if (i != md.digest().size - 1) strSha1.append(":")
                }
                val something = String(Base64.encode(md.digest(), 0))
                Log.e(TAG, "GetAppKeyHash: $something")
                Log.e(TAG, "GetAppKeyHash: $strSha1")
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "GetAppKeyHash : %s", something)
                m_Log!!.write(AndroidLog.LOGTYPE_INFO, "GetAppKeyHash : %s", strSha1.toString())
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString())
        }
    }

    fun ShowImageDialog() {
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          Dialog Execute : ")
        m_cImgDlg = ImageDialog(m_cAct!!)
        m_cImgDlg!!.show(m_strTouchUrl!!)
    }

    fun IsSwipeActionPage(strUrl: String): Boolean {
        var bRes = false
        val nArrSize = m_strSwipePageArray.size
        for (s in m_strSwipePageArray) {
            if (strUrl.compareTo(m_strLoadUrl + s, ignoreCase = true) == 0) {
                bRes = true
                break
            }
        }
        return bRes
    }

    fun SetSwipeRefresh() {
        mSwipeRefreshLayout = findViewById<View>(R.id.swipeRefresh) as SwipeRefreshLayout
        //색상지정
        mSwipeRefreshLayout!!.setColorSchemeColors(Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY)
        mSwipeRefreshLayout!!.setOnRefreshListener {
            mSwipeRefreshLayout!!.isRefreshing = true
            // 0.1초 후에 페이지를 리로딩하고 동글뱅이를 닫아준다.setRefreshing(false);
            Handler().postDelayed({
                mWebView!!.reload()
                mSwipeRefreshLayout!!.isRefreshing = false
            }, 1) // 1/1000초
        }
        mSwipeRefreshLayout!!.isEnabled = false
    }

    fun RunAuthPermission(nRequestCode: Int, grantResults: IntArray) {
        when (nRequestCode) {
            MY_PERMISSIONS_REQUEST_CALL_PHONE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허가
                // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                SendPhoneCall(m_strPhoneNum)
            } // 권한 거부
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
            }
            MY_PERMISSIONS_REQUEST_CAMERA -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허가
                // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                GetCameraPermission()
            }
        }
    }

    fun SendMessageToWeb(nType: Int, strMsg: String) {
        var strMsg = strMsg
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "SendMessageToWeb")
        var strURL = ""
        when (nType) {
            UTYPE_POSTMSG_KAKAO, UTYPE_POSTMSG_NAVER, UTYPE_POSTMSG_GOOGLE ->                 //strURL = m_strLoadUrl + "login/mobile_naver";
                strURL = m_strLoadUrl + "login/sns"
            UTYPE_POSTMSG_INTRO -> strURL = m_strLoadUrl + "start"
        }
        strMsg = "data=$strMsg"
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          : SendURL - %s", strURL)
        m_Log!!.write(AndroidLog.LOGTYPE_INFO, "          : SendMsg - %s", strMsg)
        mWebView!!.postUrl(strURL, strMsg.toByteArray())
    }

    private fun getResultUri(data: Intent?): Uri? {
        var result: Uri? = null
        if (data == null || TextUtils.isEmpty(data.dataString)) {
            // If there is not data, then we may have taken a photo
            if (mCameraPhotoPath != null) result = Uri.parse(mCameraPhotoPath)
        } else {
            var filePath: String? = ""
            filePath =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) data.dataString else "file:" + RealPathUtil.getRealPath(
                    this,
                    data.data!!
                )
            result = Uri.parse(filePath)
        }
        return result
    }

    private fun initAlertDialog() {
        m_AlertDlg = AlertDialog.Builder(this).create()

        m_AlertDlg.setButton(DialogInterface.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { dialog, which -> })
        m_ExitDlg = AlertDialog.Builder(this).create()
        m_ExitDlg.setMessage("종료 하시겠습니까?")
        m_ExitDlg.setButton(DialogInterface.BUTTON_POSITIVE, "예", DialogInterface.OnClickListener { dialog, which ->
                val cThread: Thread = object : Thread() {
                    override fun run() {
                        AndroidHttpUtil.downloadData(m_strLoadUrl + "login/bye")
                    }
                }
                cThread.start()
                try {
                    cThread.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                if (m_cAct != null) m_cAct!!.finish() else System.exit(0)
            })
        m_ExitDlg.setButton(
            DialogInterface.BUTTON_NEGATIVE, "아니오",
            DialogInterface.OnClickListener { dialog, which -> // History 삭제
                mWebView!!.clearHistory()
            })
        m_CallDlg = AlertDialog.Builder(this).create()
        m_CallDlg.setButton(
            DialogInterface.BUTTON_POSITIVE, "통화",
            DialogInterface.OnClickListener { dialog, which -> SendPhoneCall(m_strPhoneNum) })
        m_CallDlg.setButton(
            DialogInterface.BUTTON_NEGATIVE, "취소",
            DialogInterface.OnClickListener { dialog, which -> })
    }

    fun setWebViewSetting() {
        val intent = intent
        val param = intent.dataString
        mWebView!!.settings.allowUniversalAccessFromFileURLs = true
        mWebView!!.settings.allowFileAccessFromFileURLs = true
        mWebView!!.settings.domStorageEnabled = true
        mWebView!!.settings.javaScriptEnabled = true
        mWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView!!.settings.setSupportMultipleWindows(true)
        mWebView!!.settings.allowFileAccess = true
        mWebView!!.settings.allowContentAccess = true
        mWebView!!.settings.loadsImagesAutomatically = true
        mWebView!!.settings.pluginState = WebSettings.PluginState.ON
        mWebView!!.settings.mediaPlaybackRequiresUserGesture = false // 동영상 자동 재생
        mWebView!!.settings.saveFormData = true
        mWebView!!.clearCache(true)
        mWebView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        mWebView!!.settings.setAppCacheEnabled(true)
        mWebView!!.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) mWebView!!.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            null
        ) else mWebView!!.setLayerType(
            View.LAYER_TYPE_SOFTWARE, null
        )
        mWebView!!.isHorizontalScrollBarEnabled = false
        mWebView!!.isVerticalScrollBarEnabled = false
        mWebView!!.addJavascriptInterface(AndroidBridge(), "hirooapp")
        mWebView!!.webChromeClient = OneThingWebChromeClient()
        mWebView!!.webViewClient = OneThingWebViewClient()
        mWebView!!.webViewClient = MyWebViewClient()
        var strUrl = ""
        strUrl =
            if (param == null || param == "") m_strLoadUrl else param.substring(param.indexOf("hiroo"))
        mWebView!!.loadUrl(strUrl)
        mWebView!!.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            GetWriteFilePermission()
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file")
                var fileName = contentDisposition.replace("inline; filename=", "")
                fileName = fileName.replace("\"".toRegex(), "")
                request.setTitle(fileName)
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val dm =
                    getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mWebView!!.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                Log.v(javaClass.name, "onTouch : " + event.action)
                val result = (v as WebView).hitTestResult
                gestureScanner!!.onTouchEvent(event)
                Log.e(TAG, "onTouch: $event")
                if (result != null) {
                    Log.e(TAG, "onTouch: " + result.extra)
                    if (result.type == HitTestResult.IMAGE_TYPE) {
                        m_strTouchUrl = result.extra
                        Log.v(javaClass.name, "onTouch : WebView.HitTestResult.IMAGE_TYPE ")
                    }
                }
                return false
            }
        })
    }

    fun GetCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSIONS_REQUEST_CAMERA
                )
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }
            return
        }
    }

    fun GetWriteFilePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                )
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }
            return
        }
    }

    fun SendPhoneCall(strPhoneNumber: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CALL_PHONE
                )
            ) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    MY_PERMISSIONS_REQUEST_CALL_PHONE
                )
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }
            return
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$strPhoneNumber"))
        m_cAct!!.startActivity(intent)
    }

    fun Sleep(nTime: Int) {
        try {
            Thread.sleep(nTime.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //endregion
    //region  unused overrides
    override fun onShowPress(e: MotionEvent) {
        Log.v(javaClass.name, "onShowPress : " + e.action)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        Log.v(javaClass.name, "onSingleTapUp : " + e.action)
        return false
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        Log.v(javaClass.name, "onScroll : ")
        return false
    }

    //endregion
    //region  extra classes
    class LongPressChecker(context: Context?) {
        interface OnLongPressListener {
            fun onLongPressed()
        }

        private val mHandler = Handler()
        private val mLongPressCheckRunnable: LongPressCheckRunnable = LongPressCheckRunnable()
        private val mTargetView: View? = null
        private var mOnLongPressListener: OnLongPressListener? = null
        private var mLongPressed = false
        fun setOnLongPressListener(listener: OnLongPressListener?) {
            mOnLongPressListener = listener
        }

        fun stopTimeout() {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "Stop Timeout : ")
            Log.v(javaClass.name, "Stop Timeout : ")
            if (!mLongPressed) mHandler.removeCallbacks(mLongPressCheckRunnable)
        }

        private inner class LongPressCheckRunnable : Runnable {
            override fun run() {
                if (m_cImgDlg != null) {
                    if (m_cImgDlg!!.IsShowing()) {
                        if (m_cImgDlg!!.IsDismiss()) {
                            m_cImgDlg!!.dismiss()
                            m_cImgDlg = null
                        }
                        stopTimeout()
                        return
                    }
                }
                if (!mLongPressed) {
                    mLongPressed = true
                    if (mOnLongPressListener != null) {
                        Log.v(javaClass.name, "mOnLongPressListener != null : ")
                        mTargetView!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        mOnLongPressListener!!.onLongPressed()
                    }
                }
            }
        }

        init {
            if (Looper.myLooper() != Looper.getMainLooper()) throw RuntimeException()
            val mLongPressTimeout =
                ViewConfiguration.getLongPressTimeout() + 1500 // 1초 : ViewConfiguration.getLongPressTimeout() = 500 milliseconds.
            m_Log!!.write(
                AndroidLog.LOGTYPE_INFO,
                "LongPressTimeout : %s",
                mLongPressTimeout.toString()
            )
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.d("kakaolink", url)
            return if (url.startsWith(Companion.INTENT_PROTOCOL_START)) {
                val customUrlStartIndex = Companion.INTENT_PROTOCOL_START.length
                val customUrlEndIndex = url.indexOf(Companion.INTENT_PROTOCOL_INTENT)
                if (customUrlEndIndex < 0) {
                    false
                } else {
                    val customUrl = url.substring(customUrlStartIndex, customUrlEndIndex)
                    try {
                        baseContext.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(customUrl)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: ActivityNotFoundException) {
                        Log.d("kakaolink", "error")
                        val packageStartIndex =
                            customUrlEndIndex + Companion.INTENT_PROTOCOL_INTENT.length
                        val packageEndIndex = url.indexOf(Companion.INTENT_PROTOCOL_END)
                        val packageName = url.substring(
                            packageStartIndex,
                            if (packageEndIndex < 0) url.length else packageEndIndex
                        )
                        val cIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Companion.GOOGLE_PLAY_STORE_PREFIX + packageName)
                        )
                        cIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        baseContext.startActivity(cIntent)
                        //getBaseContext().startActivity(new Intent(Intent.ACTION_VIEW,  Uri.parse(GOOGLE_PLAY_STORE_PREFIX + packageName)));
                    }
                    true
                }
            } else {
                false
            }
        }

        companion object {
            const val INTENT_PROTOCOL_START = "intent:"
            const val INTENT_PROTOCOL_INTENT = "#Intent;"
            const val INTENT_PROTOCOL_END = ";end;"
            const val GOOGLE_PLAY_STORE_PREFIX = "market://details?id="
        }
    }

    private inner class OneThingWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            //return super.shouldOverrideUrlLoading(view, url);
            var url = url
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "shouldOverrideUrlLoading = %s", url)
            if (url.startsWith("mailto:")) {
                //mailto:ironnip@test.com
            } else if (url.startsWith("sms:")) {
            } else if (url.startsWith("tel:")) {
            } else if (url.startsWith("ispmobile:")) {
                m_strPackageName = "kvp.jjy.MispAndroid320"
                if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                    val strDownUrl =
                        "https://play.google.com/store/apps/details?id=$m_strPackageName"
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    cIntent.data = Uri.parse(strDownUrl)
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                    onBackPressed()
                } else {
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    cIntent.data = Uri.parse(url)
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                }
            } else if (url.startsWith("intent:")) {
                val strIntentData = url.split(";").toTypedArray()
                try {
                    var cRunIntent: Intent? = null
                    try {
                        cRunIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    } catch (ex: URISyntaxException) {
                    }
                    for (i in strIntentData.indices) {
                        if (strIntentData[i].startsWith("package=")) {
                            try {
                                val cIntent = Intent()
                                cIntent.action = Intent.ACTION_VIEW
                                val strPackageName = strIntentData[i].split("=").toTypedArray()
                                val strPackageDownUrl =
                                    "https://play.google.com/store/apps/details?id=" + strPackageName[1]
                                m_strPackageName = strPackageName[1]
                                if (isPackageInstalled(m_strPackageName, m_cAct)) break else {
                                    cIntent.data = Uri.parse(strPackageDownUrl)
                                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                                }
                            } catch (ae: ActivityNotFoundException) {
                            }
                        }
                    }
                    if (isPackageInstalled(m_strPackageName, m_cAct)) {
                        val uri = Uri.parse(cRunIntent!!.dataString)
                        cRunIntent = Intent(Intent.ACTION_VIEW, uri)
                        m_cAct!!.startActivityForResult(cRunIntent, RESULT_OK)
                    }
                } catch (ae: ActivityNotFoundException) {
                }
            } else {
                val strCreateUrl = view.url
                if (strCreateUrl!!.startsWith("http://tpay.billgate.net") ||
                    strCreateUrl.startsWith("https://pay.billgate.net")
                ) {
                    if (url.startsWith("lguthepay-xpay:")) {
                        m_strPackageName = "com.lguplus.paynow"
                        if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                            val strDownUrl =
                                "https://play.google.com/store/apps/details?id=$m_strPackageName"
                            val cIntent = Intent()
                            cIntent.action = Intent.ACTION_VIEW
                            cIntent.data = Uri.parse(strDownUrl)
                            m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                        } else {
                            val cIntent = Intent()
                            cIntent.action = Intent.ACTION_VIEW
                            cIntent.data = Uri.parse(url)
                            m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                        }
                    } else if (url.startsWith("http:") ||
                        url.startsWith("https:")
                    ) {
                        view.loadUrl(url)
                    } else if (url.startsWith("market:")) {   //market://details?id=kvp.jjy.MispAndroid320
                        val strIntentData = url.split("'?").toTypedArray()
                        for (i in strIntentData.indices) {
                            if (strIntentData[i].startsWith("id=")) {
                                val strPackageName = strIntentData[i].split("=").toTypedArray()
                                m_strPackageName = strPackageName[1]
                            }
                        }
                        val cIntent = Intent()
                        cIntent.action = Intent.ACTION_VIEW
                        url = url.replaceFirst(
                            "market://".toRegex(),
                            "https://play.google.com/store/apps/"
                        )
                        cIntent.data = Uri.parse(url)
                        m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                    } else if (url.startsWith("intent:")) {
                        val strIntentData = url.split(";").toTypedArray()
                        try {
                            var cRunIntent: Intent? = null
                            try {
                                cRunIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            } catch (ex: URISyntaxException) {
                            }
                            for (i in strIntentData.indices) {
                                if (strIntentData[i].startsWith("package=")) {
                                    try {
                                        val cIntent = Intent()
                                        cIntent.action = Intent.ACTION_VIEW
                                        val strPackageName =
                                            strIntentData[i].split("=").toTypedArray()
                                        val strPackageDownUrl =
                                            "https://play.google.com/store/apps/details?id=" + strPackageName[1]
                                        m_strPackageName = strPackageName[1]
                                        if (isPackageInstalled(
                                                m_strPackageName,
                                                m_cAct
                                            )
                                        ) break else {
                                            cIntent.data = Uri.parse(strPackageDownUrl)
                                            m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                                        }
                                    } catch (ae: ActivityNotFoundException) {
                                    }
                                }
                            }
                            if (isPackageInstalled(m_strPackageName, m_cAct)) {
                                val uri = Uri.parse(cRunIntent!!.dataString)
                                cRunIntent = Intent(Intent.ACTION_VIEW, uri)
                                m_cAct!!.startActivityForResult(cRunIntent, RESULT_OK)
                            }
                        } catch (ae: ActivityNotFoundException) {
                        }
                    }
                    return true
                } else if (url.startsWith("market:")) {
                    //market://details?id=kvp.jjy.MispAndroid320
                    val strIntentData = url.split("'?").toTypedArray()
                    for (i in strIntentData.indices) {
                        if (strIntentData[i].startsWith("id=")) {
                            val strPackageName = strIntentData[i].split("=").toTypedArray()
                            m_strPackageName = strPackageName[1]
                        }
                    }
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    url = url.replaceFirst(
                        "market://".toRegex(),
                        "https://play.google.com/store/apps/"
                    )
                    cIntent.data = Uri.parse(url)
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                } else if (url.startsWith("lguthepay-xpay:")) {
                    m_strPackageName = "com.lguplus.paynow"
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                        val strDownUrl =
                            "https://play.google.com/store/apps/details?id=$m_strPackageName"
                        cIntent.data = Uri.parse(strDownUrl)
                    } else {
                        cIntent.data = Uri.parse(url)
                    }
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                } else {
                    view.loadUrl(url)
                }
            }
            return true
        }

        private fun isPackageInstalled(packagename: String, context: Context?): Boolean {
            val pm = context!!.packageManager
            return try {
                pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onPageStarted = %s", url)
            if (IsSwipeActionPage(url)) mSwipeRefreshLayout!!.isEnabled =
                true else mSwipeRefreshLayout!!.isEnabled =
                false
            m_bIsLoading = true
            if (m_cDlgLoading == null) {
                m_cDlgLoading = LoadingDialog(m_cAct!!)
                m_cDlgLoading!!.show()
                m_cDlgLoading!!.setOnCancelListener { onBackPressed() }
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
            */super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onPageFinished = %s", url)
            m_bIsLoading = false
            if (m_cDlgLoading != null) {
                if (m_cDlgLoading!!.isShowing) {
                    m_cDlgLoading!!.dismiss()
                    m_cDlgLoading = null
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
            */if (url.endsWith(".mp4")) {
                val i = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(url)
                i.setDataAndType(uri, "video/mp4")
                startActivity(i)
            }

            //String strMsg = "안드로이드로 부터 온 메시지";
            //mWebView.loadUrl("javascript:getmessage('"+ strMsg +"')");

            //view.loadUrl("javascript:window.hirooapp.getHtml(document.getElementsByTagName('html')[0].innerHTML);"); //<html></html> 사이에 있는 모든 html을 넘겨준다.
            super.onPageFinished(view, url)
        }

        override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onFormResubmission = %s", view.url)
        }

        override fun onLoadResource(view: WebView, url: String) {
            //m_Log.write(AndroidLog.LOGTYPE_INFO, "onLoadResource = %s", url);
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedClientCertRequest = %s", view.url)
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedHttpError = %s", view.url)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_Log!!.write(
                    AndroidLog.LOGTYPE_INFO,
                    "onReceivedHttpError = %s",
                    request.url.toString()
                )
                m_Log!!.write(
                    AndroidLog.LOGTYPE_INFO,
                    "onReceivedHttpError = %d",
                    errorResponse.statusCode
                )
            }
        }

        override fun onTooManyRedirects(view: WebView, cancelMsg: Message, continueMsg: Message) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onTooManyRedirects = %s", view.url)
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", view.url)
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %d", errorCode)
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", description)
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onReceivedError = %s", failingUrl)
            if (m_cDlgLoading != null) {
                if (m_cDlgLoading!!.isShowing) {
                    m_cDlgLoading!!.dismiss()
                    m_cDlgLoading = null
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
            */super.onReceivedError(view, errorCode, description, failingUrl)
        }

        override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onScaleChanged = %s", view.url)
            super.onScaleChanged(view, oldScale, newScale)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            val builder = AlertDialog.Builder(m_cAct)
            builder.setMessage("SSL 인증서가 올바르지 않습니다. 계속 진행하시겠습니까?")
            builder.setPositiveButton(
                "continue"
            ) { dialog, which -> handler.proceed() }
            builder.setNegativeButton(
                "cancel"
            ) { dialog, which -> handler.cancel() }
            val dialog = builder.create()
            dialog.show()

            /*
            handler.proceed();
            super.onReceivedSslError(view, handler, error);
            */
        }
    }

    inner class OneThingWebChromeClient : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView,
            dialog: Boolean,
            userGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            view.removeAllViews()
            //m_bCreateWindow = true;
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "onCreateWindow = %s", view.url)
            val strCreateUrl = view.url
            mChildWebView = WebView(view.context)
            mChildWebView!!.settings.allowUniversalAccessFromFileURLs = true
            mChildWebView!!.settings.allowFileAccessFromFileURLs = true
            mChildWebView!!.settings.domStorageEnabled = true
            mChildWebView!!.settings.javaScriptEnabled = true
            mChildWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
            mChildWebView!!.settings.setSupportMultipleWindows(true)
            mChildWebView!!.settings.allowFileAccess = true
            mChildWebView!!.settings.allowContentAccess = true
            mChildWebView!!.settings.pluginState = WebSettings.PluginState.ON
            mChildWebView!!.settings.mediaPlaybackRequiresUserGesture = false
            mChildWebView!!.settings.saveFormData = true

            //mChildWebView.getSettings().setSupportZoom(true);
            //mChildWebView.getSettings().setBuiltInZoomControls(true);
            mChildWebView!!.clearCache(false)
            mChildWebView!!.settings.setAppCacheEnabled(true)
            mChildWebView!!.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
            mChildWebView!!.settings.useWideViewPort = true
            mChildWebView!!.settings.loadWithOverviewMode = true
            mChildWebView!!.webChromeClient = OneThingWebChromeClient()
            mChildWebView!!.webViewClient = OneThingWebViewClient()
            mChildWebView!!.webViewClient = MyWebViewClient()
            mChildWebView!!.addJavascriptInterface(AndroidBridge(), "hirooapp")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) mChildWebView!!.setLayerType(
                View.LAYER_TYPE_HARDWARE, null
            ) else mChildWebView!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            mChildWebView!!.isHorizontalScrollBarEnabled = false
            mChildWebView!!.isVerticalScrollBarEnabled = false
            //mChildWebView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            val lyWebViewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            mChildWebView!!.layoutParams = lyWebViewParams
            view.addView(mChildWebView)
            val transport = resultMsg.obj as WebViewTransport
            transport.webView = mChildWebView
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(view: WebView) {
            super.onCloseWindow(view)
            view.removeView(view) // 화면에서 제거
            view.visibility = View.GONE
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            //return super.onJsAlert(view, url, message, result);
            m_AlertDlg!!.setMessage(message)
            m_AlertDlg!!.show()
            result.confirm()
            return true
        }

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            // TODO Auto-generated method stub
            //return super.onJsConfirm(view, url, message, result);
            m_AlertDlg!!.setMessage("Confirm : $message")
            m_AlertDlg!!.show()
            return true
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {}

        // For Android Version < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
            //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
            mUploadMessage = uploadMsg
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = TYPE_IMAGE
            startActivityForResult(intent, INPUT_FILE_REQUEST_CODE)
        }

        // For 3.0 <= Android Version < 4.1
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String?) {
            //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
            openFileChooser(uploadMsg, acceptType, "")
        }

        // For 4.1 <= Android Version < 5.0
        fun openFileChooser(
            uploadFile: ValueCallback<Uri?>?,
            acceptType: String?,
            capture: String?
        ) {
            mUploadMessage = uploadFile
            imageChooser()
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams
        ): Boolean {
            println("WebActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3")
            if (mFilePathCallback != null) mFilePathCallback!!.onReceiveValue(null)
            mFilePathCallback = filePathCallback
            //fileChooser();
            imageChooser()
            return true
        }

        private fun fileChooser() {
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = TYPE_IMAGE
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser")
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
        }

        private fun imageChooser() {
            GetCameraPermission()
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(javaClass.name, "Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                } else {
                    takePictureIntent = null
                }
            }
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            //contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            contentSelectionIntent.type = TYPE_IMAGE
            val intentArray: Array<Intent?>
            intentArray = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "이미지 선택")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
        }

        @Throws(IOException::class)
        private fun createImageFile(): File {
            // Create an image file name
            val calendar = Calendar.getInstance()
            val timeStamp = String.format(
                "%04d-%02d-%02d_%02d:%02d:%02d", calendar[Calendar.YEAR],
                calendar[Calendar.MONTH] + 1,
                calendar[Calendar.DAY_OF_MONTH],
                calendar[Calendar.HOUR_OF_DAY],
                calendar[Calendar.MINUTE],
                calendar[Calendar.SECOND]
            )
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
            )
        }
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun getHtml(html: String?) { //위 자바스크립트가 호출되면 여기로 html이 반환됨
            println(html)
        }

        @JavascriptInterface
        fun callAndroid(arg: String?) {
            ReceiveToWebRequest(arg)
        }

        @JavascriptInterface
        fun callAndroid(arg: String, arga: String): String {
            return SetResultData(arg, arga, "")
        }

        @JavascriptInterface
        fun callAndroid(arg: String, arga: String, argb: String): String {
            return SetResultData(arg, arga, argb)
        }

        @JavascriptInterface
        fun ReturnToWeb(strReturnData: String) {
            m_strReturnData = strReturnData
            mhandler.post { mWebView!!.loadUrl("javascript:ReturnMobile('$m_strReturnData')") }
        }

        @JavascriptInterface
        fun SharedUrl(strUrl: String?, strTitle: String?) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, strTitle)
            intent.putExtra(Intent.EXTRA_TEXT, strUrl)

            // Title of intent
            val iShare = Intent.createChooser(intent, "공유하기")
            startActivity(iShare)
        }

        fun ReceiveToWebRequest(strRequestData: String?) {
            try {
                if (strRequestData != null) {
                    val jsonObj = JSONObject(strRequestData)
                    var strApi = ""
                    var strData = ""
                    var strOption = ""
                    strApi = jsonObj.getString("Api")
                    strData = jsonObj.getString("Data")
                    strOption = jsonObj.getString("Option")
                    if (strApi.compareTo(
                            "CheckApp",
                            ignoreCase = true
                        ) == 0 || strApi.compareTo(
                            "GetGPSInfo",
                            ignoreCase = true
                        ) == 0 || strApi.compareTo(
                            "ExternSNSAccount",
                            ignoreCase = true
                        ) == 0 || strApi.compareTo(
                            "GetNetworkInfo",
                            ignoreCase = true
                        ) == 0 || strApi.compareTo(
                            "MemberWithdraw",
                            ignoreCase = true
                        ) == 0 || strApi.compareTo("GetContactInfo", ignoreCase = true) == 0
                    ) {
                        var strRes = ""
                        strRes = ReturnDataToWeb(strApi, strData)
                        ReturnToWeb(strRes)
                    } else {
                        if (strApi.compareTo("SaveAutoLogin", ignoreCase = true) == 0) WriteEnv(
                            strData
                        ) else if (strApi.compareTo("PhoneCall", ignoreCase = true) == 0) {
                            m_strPhoneNum = strData
                            SendPhoneCall(m_strPhoneNum)
                        } else if (strApi.compareTo("RunLink", ignoreCase = true) == 0) {
                            m_strRunLink = strData
                            mhandler.post {
                                val cIntent = Intent()
                                cIntent.action = Intent.ACTION_VIEW
                                cIntent.data = Uri.parse(m_strRunLink)
                                m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                            }
                        } else if (strApi.compareTo("CreateWindow", ignoreCase = true) == 0) {
                            m_strCreateUrl = m_strLoadUrl + strData
                            m_strOptions = strOption
                            mhandler.post {
                                val bundle = Bundle()
                                bundle.putInt(
                                    CreateWebViewActivity.PARAM_TYPE,
                                    CreateWebViewActivity.PARAM_TYPE_GENERAL
                                )
                                bundle.putString(CreateWebViewActivity.PARAM_URL, m_strCreateUrl)
                                bundle.putString(CreateWebViewActivity.PARAM_OPTIONS, m_strOptions)
                                val cIntent = Intent(
                                    this@WebActivity,
                                    CreateWebViewActivity::class.java
                                )
                                cIntent.putExtras(bundle)
                                startActivity(cIntent)
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        fun SetResultData(strArg: String, strArgs: String, strArg1: String): String {
            var strRes = ""
            if (strArg.compareTo("CheckApp", ignoreCase = true) == 0) strRes =
                "hiroo," + FirebaseInstanceId.getInstance().token else if (strArg.compareTo(
                    "GetDeviceInfo",
                    ignoreCase = true
                ) == 0
            ) {
                //모바일토큰, 디바이스정보, 모델정보, 가로사이즈, 세로사이즈
                strRes =
                    FirebaseInstanceId.getInstance().token + "," + m_cDevice!!.getSdkVersion() + "," + m_cDevice!!.getModel() + "," + ReadEnvForAutoLogin() // + ","+m_cDevice.GetWidthPx() + "," + m_cDevice.GetHeightPx();
                SendMessageToWeb(UTYPE_POSTMSG_INTRO, strRes)
            } else if (strArg.compareTo("SaveLoginID", ignoreCase = true) == 0) strRes =
                "True" else if (strArg.compareTo("SaveAutoLogin", ignoreCase = true) == 0) {
                WriteEnv(strArgs)
                strRes = "True"
            } else if (strArg.compareTo("PhoneCall", ignoreCase = true) == 0) {
                m_strPhoneNum = strArgs
                SendPhoneCall(m_strPhoneNum)
                strRes = "True"
            } else if (strArg.compareTo("ExternSNSAccount", ignoreCase = true) == 0) {
                if (strArgs.compareTo("kakao", ignoreCase = true) == 0) {
                    showProgressDialog()
                    m_Log!!.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : KAKAO")
                    //if( !m_bIsProcKakao )
                    run {
                        m_bIsProcKakao = true
                        m_Log!!.write(
                            AndroidLog.LOGTYPE_INFO,
                            "          : START"
                        )
                        m_cKakao = KakaoLogin(mMainHandler, this@WebActivity)
                        m_cKakao!!.KakaoLoginStart()
                        strRes = m_cKakao!!.ResultData()
                    }
                } else if (strArgs.compareTo("google", ignoreCase = true) == 0) {
                    showProgressDialog()
                    m_Log!!.write(AndroidLog.LOGTYPE_INFO, "ExternSNSAccount : GOOGLE")
                    //if( !m_bIsProcGoogle )
                    run {
                        m_bIsProcGoogle = true
                        m_Log!!.write(
                            AndroidLog.LOGTYPE_INFO,
                            "          : START"
                        )
                        GoogleActivity.setHandler(mMainHandler)
                        val i = Intent(this@WebActivity, GoogleActivity::class.java)
                        startActivity(i)
                        var nTimeout = 0
                        while (m_nStatus != OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE) {
                            if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED) {
                                GoogleActivity.setHandler(mMainHandler)
                            }
                            if (m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED ||
                                m_nStatus == OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED
                            ) break
                            Sleep(1000)
                            if (nTimeout > 10) break
                            nTimeout++
                        }
                        m_Log!!.write(
                            AndroidLog.LOGTYPE_INFO,
                            "          : END"
                        )
                        hideProgressDialog()
                        strRes = GoogleActivity.resultData()
                        m_bIsProcGoogle = false
                    }
                } else if (strArgs.compareTo("naver", ignoreCase = true) == 0) {
                    if (!m_bIsProcNaver) {
                        m_bIsProcNaver = true
                        m_cNaver = NaverLogin(mMainHandler, this@WebActivity)
                        strRes = m_cNaver!!.ResultData()
                    }
                } else strRes = "Unknown Interface"
            } else if (strArg.compareTo("RunLink", ignoreCase = true) == 0) {
                m_strRunLink = strArgs
                mhandler.post {
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    cIntent.data = Uri.parse(m_strRunLink)
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                }
            } else if (strArg.compareTo("CreateWindow", ignoreCase = true) == 0) {
                m_strCreateUrl = m_strLoadUrl + strArgs
                m_strOptions = strArg1
                mhandler.post {
                    val bundle = Bundle()
                    bundle.putInt(
                        CreateWebViewActivity.PARAM_TYPE,
                        CreateWebViewActivity.PARAM_TYPE_GENERAL
                    )
                    bundle.putString(CreateWebViewActivity.PARAM_URL, m_strCreateUrl)
                    bundle.putString(CreateWebViewActivity.PARAM_OPTIONS, m_strOptions)
                    val cIntent = Intent(this@WebActivity, CreateWebViewActivity::class.java)
                    cIntent.putExtras(bundle)
                    startActivity(cIntent)
                }
            } else strRes = "Unknown Interface,"
            return strRes
        }
    }

    inner class SendMassgeHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            m_Log!!.write(AndroidLog.LOGTYPE_INFO, "handleMessage : ")
            hideProgressDialog()
            when (msg.what) {
                OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_KAKAO_COMPLETE"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_COMPLETE
                    SendMessageToWeb(UTYPE_POSTMSG_KAKAO, m_cKakao!!.ResultDataForJson())
                    m_bIsProcKakao = false
                }
                OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_KAKAO_SESSIONCLOSED"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONCLOSED
                }
                OneThingTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_KAKAO_NOTSIGNEDUP"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_NOTSIGNEDUP
                }
                OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_KAKAO_SESSIONOPENFAILED"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_SESSIONOPENFAILED
                }
                OneThingTypes.STATUS_LOGIN_KAKAO_UNKNOWN -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_KAKAO_UNKNOWN"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_KAKAO_UNKNOWN
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_COMPLETE"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_COMPLETE
                    SendMessageToWeb(UTYPE_POSTMSG_GOOGLE, GoogleActivity.resultData())
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_SIGNINREQUIRED"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_SIGNINREQUIRED
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_ONDESTROY -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_ONDESTROY"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_ONDESTROY
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_AUTHFAILED"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_AUTHFAILED
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_CONNECTIONFAILED"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_CONNECTIONFAILED
                }
                OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_GOOGLE_UNKNOWN"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_GOOGLE_UNKNOWN
                }
                OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_NAVER_COMPLETE"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_COMPLETE
                    SendMessageToWeb(UTYPE_POSTMSG_NAVER, m_cNaver!!.ResultDataForJson())
                    m_bIsProcNaver = false
                }
                OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_NAVER_USERCANCEL"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_USERCANCEL
                }
                OneThingTypes.STATUS_LOGIN_NAVER_UNKNOWN -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_NAVER_UNKNOWN"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_UNKNOWN
                }
                OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN -> {
                    m_Log!!.write(
                        AndroidLog.LOGTYPE_INFO,
                        "handleMessage : STATUS_LOGIN_NAVER_DELETETOKEN"
                    )
                    m_nStatus = OneThingTypes.STATUS_LOGIN_NAVER_DELETETOKEN
                }
                else -> {
                    m_Log!!.write(AndroidLog.LOGTYPE_INFO, "handleMessage : UNKNOWN(%d)", msg.what)
                    m_nStatus = msg.what
                }
            }
        }
    } //endregion

    companion object {
        //region  vars
        private const val TAG = "WebActivity"
        private const val TYPE_IMAGE = "image/*"
        private const val SWIPE_MIN_DISTANCE = 400
        private const val SWIPE_MAX_OFF_PATH = 1000
        private const val SWIPE_THRESHOLD_VELOCITY = 500
        private const val INPUT_FILE_REQUEST_CODE = 1000
        private var m_Log: AndroidLog? = null
        var m_cImgDlg: ImageDialog? = null
    }
}

