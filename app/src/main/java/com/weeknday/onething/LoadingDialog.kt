package com.weeknday.onething

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView

class LoadingDialog(context: Context) : Dialog(context) {
    private var m_ivImage: ImageView? = null
    private var m_cAnim: AnimationDrawable? = null
    fun InitCtrl() {
        m_ivImage = findViewById(R.id.dlg_ivIcon)
    }

    override fun onCreate(savedInstanceState: Bundle) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dlg_loading)
        InitCtrl()
        m_ivImage?.visibility = View.VISIBLE
        m_ivImage?.setBackgroundResource(R.drawable.loading_animation)
        m_ivImage?.setBackgroundColor(Color.TRANSPARENT)
        m_cAnim = m_ivImage!!.drawable as AnimationDrawable

    }

    override fun show() {
        super.show()
        m_cAnim?.start()
    }

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}