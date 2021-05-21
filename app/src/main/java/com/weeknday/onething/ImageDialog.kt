package com.weeknday.onething

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class ImageDialog(private val m_Context: Context) {
    private val m_Dlg: Dialog = Dialog(m_Context)
    private var m_ivImage: ImageView? = null
    private var m_UrlImage: Bitmap? = null
    private var m_strUrl = ""
    private var m_bIsDismiss = false
    var m_cPVAttacher: PhotoViewAttacher? = null
    private val mhandler = Handler()
    private val mProgressDialog: ProgressDialog? = null
    private fun hideProgressDialog() {
        mhandler.post {
            if (mProgressDialog != null && mProgressDialog.isShowing) {
                mProgressDialog.hide()
            }
        }
    }

    fun IsShowing(): Boolean {
        val bRes: Boolean
        bRes = m_Dlg.isShowing
        return bRes
    }

    private var mWVProgressDialog: ProgressDialog? = null
    fun show(strUrl: String) {
        m_bIsDismiss = false
        m_strUrl = strUrl
        m_strUrl = m_strUrl.replaceFirst("https".toRegex(), "http")
        val view = LayoutInflater.from(m_Context).inflate(R.layout.dlg_image, null)
        m_Dlg.setContentView(view)
        val cThread: Thread = object : Thread() {
            override fun run() {
                mhandler.post {
                    if (mWVProgressDialog == null) {
                        mWVProgressDialog = ProgressDialog(m_Dlg.context)
                        mWVProgressDialog?.setMessage("Loading...")
                        mWVProgressDialog?.isIndeterminate = true
                        mWVProgressDialog?.setCanceledOnTouchOutside(false)
                        mWVProgressDialog?.setOnCancelListener {
                            m_Dlg.dismiss()
                        }
                        mWVProgressDialog?.show()
                    }
                }
                try {
                    val url = URL(m_strUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val `in` = connection.inputStream
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 2
                    m_UrlImage = BitmapFactory.decodeStream(`in`, null, options)
                    mhandler.post {
                        if (mWVProgressDialog != null) {
                            if (mWVProgressDialog!!.isShowing) {
                                mWVProgressDialog!!.dismiss()
                                mWVProgressDialog = null
                            }
                        }
                    }
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        cThread.start()
        try {
            cThread.join()
            hideProgressDialog()
            val params = m_Dlg.window?.attributes
            if (m_UrlImage != null) {
                if (params != null) {
                    params.width = WindowManager.LayoutParams.MATCH_PARENT
                }
                if (params != null) {
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                }
                m_Dlg.window!!.attributes = params as WindowManager.LayoutParams
            }
            m_ivImage = view.findViewById<View>(R.id.dlg_ivImage) as ImageView
            m_ivImage?.scaleType = ImageView.ScaleType.FIT_CENTER // 레이아웃 크기에 이미지를 맞춘다
            m_ivImage?.setOnTouchListener(ViewListener)
            m_ivImage?.setImageBitmap(m_UrlImage)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        m_ivImage!!.visibility = View.VISIBLE
        m_Dlg.show()
    }

    fun IsDismiss(): Boolean {
        return m_bIsDismiss
    }

    fun dismiss() {
        m_bIsDismiss = true
        if (m_ivImage != null) {
            m_ivImage!!.visibility = View.GONE
            m_ivImage!!.destroyDrawingCache()
        }
        if (m_cPVAttacher != null) {
            m_cPVAttacher!!.cleanup()
            m_cPVAttacher = null
        }

        // Recycle Old ImageView
        val drawable = m_ivImage!!.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            bitmap.recycle()
        }
        m_Dlg.dismiss()
    }

    private val ViewListener = OnTouchListener { v, event ->
        m_cPVAttacher = PhotoViewAttacher(m_ivImage)
        m_cPVAttacher!!.setOnViewTapListener { view, v, v2 ->
            m_bIsDismiss = true
            m_Dlg.dismiss()
        }
        false
    }

    init {
        m_Dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        m_Dlg.setOnCancelListener(DialogInterface.OnCancelListener {
            m_bIsDismiss = true
            m_Dlg.dismiss()
        })
    }
}