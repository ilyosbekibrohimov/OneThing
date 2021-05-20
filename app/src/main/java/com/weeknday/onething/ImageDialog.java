package com.weeknday.onething;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ImageDialog {
    private Context m_Context;
    private Dialog m_Dlg;
    private ImageView m_ivImage = null;

    private Bitmap m_UrlImage = null;
    private String m_strUrl = "";
    private boolean m_bIsDismiss = false;

    PhotoViewAttacher m_cPVAttacher = null;

    private final Handler mhandler = new Handler();

    private ProgressDialog mProgressDialog = null;

    public ImageDialog(Context context) {
        this.m_Context = context;

        m_Dlg = new Dialog(context);
        m_Dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        m_Dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                m_bIsDismiss = true;
                m_Dlg.dismiss();
            }
        });


    }

    private void hideProgressDialog() {
        mhandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.hide();
                }
            }
        });
    }

    public boolean IsShowing() {
        boolean bRes = false;

        if (m_Dlg != null)
            bRes = m_Dlg.isShowing();

        return bRes;
    }

    private ProgressDialog mWVProgressDialog = null;

    public void show(String strUrl) {
        m_bIsDismiss = false;

        m_strUrl = strUrl;
        m_strUrl = m_strUrl.replaceFirst("https", "http");

        View view = LayoutInflater.from(m_Context).inflate(com.weeknday.onething.R.layout.dlg_image, null);
        m_Dlg.setContentView(view);

        Thread cThread = new Thread() {
            @Override
            public void run() {
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWVProgressDialog == null) {
                            mWVProgressDialog = new ProgressDialog(m_Dlg.getContext());
                            mWVProgressDialog.setMessage("Loading...");
                            mWVProgressDialog.setIndeterminate(true);
                            mWVProgressDialog.setCanceledOnTouchOutside(false);
                            mWVProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    m_Dlg.dismiss();
                                }
                            });

                            mWVProgressDialog.show();
                        }
                    }

                });

                try {
                    URL url = new URL(m_strUrl);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream in = connection.getInputStream();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;

                    m_UrlImage = BitmapFactory.decodeStream(in, null, options);

                    mhandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mWVProgressDialog != null) {
                                if (mWVProgressDialog.isShowing()) {
                                    mWVProgressDialog.dismiss();
                                    mWVProgressDialog = null;
                                }

                            }
                        }
                    });
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        cThread.start();

        try {
            cThread.join();

            hideProgressDialog();

            WindowManager.LayoutParams params = m_Dlg.getWindow().getAttributes();

            if (m_UrlImage != null) {
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;

                m_Dlg.getWindow().setAttributes((WindowManager.LayoutParams) params);
            }

            m_ivImage = (ImageView) view.findViewById(com.weeknday.onething.R.id.dlg_ivImage);

            m_ivImage.setScaleType(ImageView.ScaleType.FIT_CENTER); // 레이아웃 크기에 이미지를 맞춘다

            m_ivImage.setOnTouchListener(ViewListener);
            m_ivImage.setImageBitmap(m_UrlImage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        m_ivImage.setVisibility(View.VISIBLE);

        m_Dlg.show();
    }

    public boolean IsDismiss() {
        return m_bIsDismiss;
    }

    public void dismiss() {
        m_bIsDismiss = true;

        if (m_ivImage != null) {
            m_ivImage.setVisibility(View.GONE);
            m_ivImage.destroyDrawingCache();
        }

        if (m_cPVAttacher != null) {
            m_cPVAttacher.cleanup();
            m_cPVAttacher = null;
        }

        // Recycle Old ImageView
        Drawable drawable = m_ivImage.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
        }

        m_Dlg.dismiss();
    }

    private final View.OnTouchListener ViewListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            m_cPVAttacher = new PhotoViewAttacher(m_ivImage);
            m_cPVAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float v, float v2) {
                    m_bIsDismiss = true;
                    m_Dlg.dismiss();
                }
            });

            return false;
        }
    };
}