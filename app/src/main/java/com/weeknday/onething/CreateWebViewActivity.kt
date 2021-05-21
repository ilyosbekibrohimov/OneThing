package com.weeknday.onething

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.View
import android.webkit.*
import android.webkit.WebView.WebViewTransport

class CreateWebViewActivity : Activity() {
    private var mWebView: WebView? = null
    private var m_cAct: Activity? = null
    private var m_strLoadUrl: String? = ""
    private var m_strIntent: String? = ""
    private var m_strOptions: String? = ""
    private var m_strPackageName = ""

    // Defined Options Flags
    private var m_bIsZoom = false

    //region overrides
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_createwebview)
        m_cAct = this
        mWebView = findViewById<View>(R.id.wvCreate) as WebView
        // Intent로 넘어온 데이터 확인
        GetIntentData(intent)

        //if( !CheckProtocal(m_strLoadUrl) )
        run {
            // WebView Setting
            //SetWebViewSetting();
            SetWebViewSettingEx()
        }
    }

    override fun onBackPressed() {
        val cBackUrlListMain: WebBackForwardList
        cBackUrlListMain = mWebView!!.copyBackForwardList()
        val nIndex = cBackUrlListMain.currentIndex
        if (nIndex > 0 || mWebView!!.canGoBack()) mWebView!!.goBackOrForward(-1) // 이전 페이지로 이동.
        else {
            mWebView!!.clearHistory()
            mWebView!!.removeView(mWebView) // 화면에서 제거
            mWebView!!.visibility = View.GONE
            mWebView!!.destroy()
            mWebView = null
            finish()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    //endregion
    private fun GetIntentData(cIntent: Intent) {
        var bRes = false
        val bundle = cIntent.extras
        if (bundle!!.containsKey(PARAM_TYPE)) {
            val nType = bundle.getInt(PARAM_TYPE)
            if (bundle.containsKey(PARAM_URL)) m_strLoadUrl = bundle.getString(PARAM_URL)
            if (bundle.containsKey(PARAM_INTENT)) m_strIntent = bundle.getString(PARAM_INTENT)
            if (bundle.containsKey(PARAM_OPTIONS)) m_strOptions = bundle.getString(PARAM_OPTIONS)
            SetFlagFromOptions(m_strOptions)
            when (nType) {
                PARAM_TYPE_GENERAL -> {
                }
                PARAM_TYPE_PAYMENTGATEWAY -> {
                }
            }
            bRes = true
        }
    }

    private fun SetFlagFromOptions(strOptions: String?) {
        val strOptionData = strOptions!!.split(",").toTypedArray()
        for (strOptionDatum in strOptionData) {
            if (strOptionDatum.compareTo("zoom", ignoreCase = true) == 0) m_bIsZoom = true
        }
    }

    private fun SetWebViewSettingEx() {
        mWebView!!.settings.javaScriptEnabled = true
        mWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView!!.settings.allowFileAccess = true
        mWebView!!.settings.allowContentAccess = true
        mWebView!!.settings.loadsImagesAutomatically = true
        mWebView!!.settings.pluginState = WebSettings.PluginState.ON
        mWebView!!.settings.loadWithOverviewMode = true
        mWebView!!.settings.useWideViewPort = true
        if (m_bIsZoom) {
            mWebView!!.settings.setSupportZoom(true)
            mWebView!!.settings.builtInZoomControls = true
            mWebView!!.settings.displayZoomControls = false
        }
        mWebView!!.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) mWebView!!.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            null
        ) else mWebView!!.setLayerType(
            View.LAYER_TYPE_SOFTWARE, null
        )
        mWebView!!.webViewClient = CreateWebViewClient()
        val strLoadUrl = m_strLoadUrl

        // WebView Loading
        mWebView!!.loadUrl(strLoadUrl!!)
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

    private inner class CreateWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            //return super.shouldOverrideUrlLoading(view, url);
            if (url.startsWith("lguthepay-xpay:")) {
                if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                    val strDownUrl =
                        "https://play.google.com/store/apps/details?id=$m_strPackageName"
                    val cIntent = Intent()
                    cIntent.action = Intent.ACTION_VIEW
                    cIntent.data = Uri.parse(strDownUrl)
                    m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                    return true
                }
            } else if (url.startsWith("market:")) {
                //market://details?id=kvp.jjy.MispAndroid320
                val strIntentData = url.split("'?").toTypedArray()
                for (strIntentDatum in strIntentData) {
                    if (strIntentDatum.startsWith("id=")) {
                        val strPackageName = strIntentDatum.split("=").toTypedArray()
                        m_strPackageName = strPackageName[1]
                    }
                }
                val cIntent = Intent()
                cIntent.action = Intent.ACTION_VIEW
                val strUrl =
                    url.replaceFirst("market://".toRegex(), "https://play.google.com/store/apps/")
                cIntent.data = Uri.parse(strUrl)
                m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
            } else if (url.startsWith("intent:")) {
                val strIntentData = url.split(";").toTypedArray()
                for (strIntentDatum in strIntentData) {
                    if (strIntentDatum.startsWith("package=")) {
                        try {
                            val cIntent = Intent()
                            cIntent.action = Intent.ACTION_VIEW
                            val strPackageName = strIntentDatum.split("=").toTypedArray()
                            val strPackageDownUrl =
                                "https://play.google.com/store/apps/details?id=" + strPackageName[1]
                            m_strPackageName = strPackageName[1]
                            cIntent.data = Uri.parse(strPackageDownUrl)
                            m_cAct!!.startActivityForResult(cIntent, RESULT_OK)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                view.loadUrl(url)
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (url.endsWith(".mp4")) {
                val i = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(url)
                i.setDataAndType(uri, "video/mp4")
                startActivity(i)
            }

            //String strMsg = "안드로이드로 부터 온 메시지";
            //mWebView.loadUrl("javascript:getmessage('"+ strMsg +"')");
            super.onPageFinished(view, url)
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
        }
    }

    companion object {
        // Intent Key
        const val PARAM_TYPE = "TYPE"
        const val PARAM_URL = "URL"
        const val PARAM_INTENT = "INTENT"
        const val PARAM_OPTIONS = "OPTIONS"

        // Intent Key Type Define
        const val PARAM_TYPE_GENERAL = 0
        const val PARAM_TYPE_PAYMENTGATEWAY = 1
    }
}