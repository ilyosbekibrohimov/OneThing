package com.weeknday.onething;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CreateWebViewActivity extends Activity {

    private WebView mWebView = null;
    private Activity m_cAct = null;

    private String m_strLoadUrl = "";
    private String m_strIntent = "";
    private String m_strOptions = "";

    private String m_strPackageName = "";


    // Defined Options Flags
    private boolean m_bIsZoom = false;


    // Intent Key
    public static final String PARAM_TYPE = "TYPE";
    public static final String PARAM_URL = "URL";
    public static final String PARAM_INTENT = "INTENT";
    public static final String PARAM_OPTIONS = "OPTIONS";

    // Intent Key Type Define
    public static final int PARAM_TYPE_GENERAL = 0;
    public static final int PARAM_TYPE_PAYMENTGATEWAY = 1;



    //region overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.weeknday.onething.R.layout.act_createwebview);

        m_cAct = this;

        mWebView = (WebView) findViewById(com.weeknday.onething.R.id.wvCreate);
        // Intent로 넘어온 데이터 확인
        GetIntentData(getIntent());

        //if( !CheckProtocal(m_strLoadUrl) )
        {
            // WebView Setting
            //SetWebViewSetting();
            SetWebViewSettingEx();
        }
    }

    @Override
    public void onBackPressed() {
        WebBackForwardList cBackUrlListMain;

        cBackUrlListMain = mWebView.copyBackForwardList();

        int nIndex = cBackUrlListMain.getCurrentIndex();



        if (nIndex > 0 || mWebView.canGoBack())
            mWebView.goBackOrForward(-1);   // 이전 페이지로 이동.
        else {
            mWebView.clearHistory();
            mWebView.removeView(mWebView);    // 화면에서 제거
            mWebView.setVisibility(View.GONE);
            mWebView.destroy();
            mWebView = null;

            this.finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    //endregion

    private void GetIntentData(Intent cIntent) {
        boolean bRes = false;

        Bundle bundle = cIntent.getExtras();

        if (bundle.containsKey(PARAM_TYPE)) {
            int nType = bundle.getInt(PARAM_TYPE);

            if (bundle.containsKey(PARAM_URL))
                m_strLoadUrl = bundle.getString(PARAM_URL);

            if (bundle.containsKey(PARAM_INTENT))
                m_strIntent = bundle.getString(PARAM_INTENT);

            if (bundle.containsKey(PARAM_OPTIONS))
                m_strOptions = bundle.getString(PARAM_OPTIONS);

            SetFlagFromOptions(m_strOptions);

            switch (nType) {
                case PARAM_TYPE_GENERAL:
                    break;
                case PARAM_TYPE_PAYMENTGATEWAY:
                    break;
            }

            bRes = true;
        }

    }

    private void SetFlagFromOptions(String strOptions) {
        String[] strOptionData = strOptions.split(",");

        for (String strOptionDatum : strOptionData) {
            if (strOptionDatum.compareToIgnoreCase("zoom") == 0)
                m_bIsZoom = true;
        }
    }

    private void SetWebViewSettingEx() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        if (m_bIsZoom) {
            mWebView.getSettings().setSupportZoom(true);
            mWebView.getSettings().setBuiltInZoomControls(true);
            mWebView.getSettings().setDisplayZoomControls(false);
        }

        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mWebView.setWebViewClient(new CreateWebViewClient());

        final String strLoadUrl = m_strLoadUrl;

        // WebView Loading
        mWebView.loadUrl(strLoadUrl);
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



    private class CreateWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            //return super.shouldOverrideUrlLoading(view, url);


            if (url.startsWith("lguthepay-xpay:")) {
                if (!isPackageInstalled(m_strPackageName, m_cAct)) {
                    String strDownUrl = "https://play.google.com/store/apps/details?id=" + m_strPackageName;

                    Intent cIntent = new Intent();
                    cIntent.setAction(Intent.ACTION_VIEW);
                    cIntent.setData(Uri.parse(strDownUrl));
                    m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                    return true;
                }
            } else if (url.startsWith("market:")) {
                //market://details?id=kvp.jjy.MispAndroid320
                String[] strIntentData = url.split("'?");
                for (String strIntentDatum : strIntentData) {
                    if (strIntentDatum.startsWith("id=")) {
                        String[] strPackageName = strIntentDatum.split("=");
                        m_strPackageName = strPackageName[1];
                    }
                }

                Intent cIntent = new Intent();
                cIntent.setAction(Intent.ACTION_VIEW);

                String strUrl = url.replaceFirst("market://", "https://play.google.com/store/apps/");
                cIntent.setData(Uri.parse(strUrl));
                m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
            } else if (url.startsWith("intent:")) {
                String[] strIntentData = url.split(";");

                for (String strIntentDatum : strIntentData) {
                    if (strIntentDatum.startsWith("package=")) {
                        try {
                            Intent cIntent = new Intent();
                            cIntent.setAction(Intent.ACTION_VIEW);

                            String[] strPackageName = strIntentDatum.split("=");
                            String strPackageDownUrl = "https://play.google.com/store/apps/details?id=" + strPackageName[1];

                            m_strPackageName = strPackageName[1];

                            cIntent.setData(Uri.parse(strPackageDownUrl));
                            m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }
                }
            } else {
                view.loadUrl(url);
            }

            return true;
        }


        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }



        public void onPageFinished(WebView view, String url) {
            if (url.endsWith(".mp4")) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(url);
                i.setDataAndType(uri, "video/mp4");
                startActivity(i);
            }

            //String strMsg = "안드로이드로 부터 온 메시지";
            //mWebView.loadUrl("javascript:getmessage('"+ strMsg +"')");

            super.onPageFinished(view, url);
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


        }
    }

    public class CreateWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            view.removeAllViews();


            WebView ChildWebView = new WebView(view.getContext());
            ChildWebView.getSettings().setJavaScriptEnabled(true);
            ChildWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            ChildWebView.getSettings().setSupportMultipleWindows(true);
            ChildWebView.getSettings().setAllowFileAccess(true);
            ChildWebView.getSettings().setAllowContentAccess(true);
            ChildWebView.getSettings().setSaveFormData(true);

            ChildWebView.clearCache(true);
            ChildWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            ChildWebView.getSettings().setAppCacheEnabled(false);
            ChildWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

            ChildWebView.getSettings().setUseWideViewPort(true);
            ChildWebView.getSettings().setLoadWithOverviewMode(true);

            ChildWebView.setWebChromeClient(new CreateWebChromeClient());
            ChildWebView.setWebViewClient(new CreateWebViewClient());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                ChildWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            else
                ChildWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            ChildWebView.setHorizontalScrollBarEnabled(false);
            ChildWebView.setVerticalScrollBarEnabled(false);

            view.addView(ChildWebView);

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(ChildWebView);
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


            result.confirm();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
        }
    }
}
