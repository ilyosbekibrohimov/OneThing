package com.weeknday.onething

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class PackageUpdateDialog(context: Context, private val m_cAct: Activity?) :
    Dialog(context) {
    //region vars
    private var m_strAction = ""
    private var m_nStatus = UTYPE_PACKAGE_STATUS_NORMAL
    private var m_bIsInit = false
    private var m_lyContents: LinearLayout? = null
    private var m_lyBtnYN: LinearLayout? = null
    private var m_lyBtnConfirm: LinearLayout? = null
    private var m_btnYes: Button? = null
    private var m_btnNo: Button? = null
    private var m_btnConfirm: Button? = null
    private var m_tvUptContents: TextView? = null
    private val m_cDlgHandler = Handler()

    //region overrides
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dlg_pkgupdate)
        if (m_bIsInit) return
        m_lyContents = findViewById<View>(R.id.dlg_pkgupt_ly_contents) as LinearLayout
        m_lyBtnYN = findViewById<View>(R.id.dlg_pkgupt_ly_btn_yn) as LinearLayout
        m_lyBtnConfirm = findViewById<View>(R.id.dlg_pkgupt_ly_btn_confirm) as LinearLayout
        m_btnYes = findViewById<View>(R.id.dlg_pkgupt_btn_y) as Button
        m_btnNo = findViewById<View>(R.id.dlg_pkgupt_btn_n) as Button
        m_btnConfirm = findViewById<View>(R.id.dlg_pkgupt_btn_confirm) as Button
        m_tvUptContents = findViewById<View>(R.id.dlg_pkgupt_tv_update_contents) as TextView
        SetBtnListener()
        SetDialogControls()
        m_bIsInit = true
    }

    //endregion
    //region functions
    private fun SetBtnListener() {
        m_btnYes!!.setOnClickListener {
            RunAction()
            dismiss()
            m_cAct?.finish()
        }
        m_btnNo!!.setOnClickListener { dismiss() }
        m_btnConfirm!!.setOnClickListener {
            RunAction()
            dismiss()
            m_cAct?.finish()
        }
    }

    fun setStatus(nLevel: Int, strAction: String) {
        when (nLevel) {
            1, 2, 3, 4, 5, 6 -> m_nStatus = UTYPE_PACKAGE_STATUS_NORMAL
            7, 8, 9 -> m_nStatus = UTYPE_PACKAGE_STATUS_EMERGENCY
        }
        m_strAction = strAction
        SetDialogControls()
    }

    fun SetDialogControls() {
        ShowContents()
        when (m_nStatus) {
            UTYPE_PACKAGE_STATUS_NORMAL -> {
                ShowBtnGroupYN(View.VISIBLE)
                ShowBtnGroupConfirm(View.GONE)
                if (m_tvUptContents != null) m_tvUptContents!!.setText(R.string.app_update_contents)
            }
            UTYPE_PACKAGE_STATUS_EMERGENCY -> {
                ShowBtnGroupYN(View.GONE)
                ShowBtnGroupConfirm(View.VISIBLE)
                if (m_tvUptContents != null) m_tvUptContents!!.setText(R.string.app_emergency_update_contents)
            }
        }
    }

    private fun ShowContents() {
        if (m_lyContents != null) m_lyContents!!.visibility = View.VISIBLE
    }

    private fun ShowBtnGroupYN(nVisibility: Int) {
        if (m_lyBtnYN != null) m_lyBtnYN!!.visibility = nVisibility
    }

    private fun ShowBtnGroupConfirm(nVisibility: Int) {
        if (m_lyBtnConfirm != null) m_lyBtnConfirm!!.visibility = nVisibility
    }

    private fun RunAction() {
        m_cDlgHandler.post {
            val cIntent = Intent()
            cIntent.action = Intent.ACTION_VIEW
            cIntent.data = Uri.parse(m_strAction)
            m_cAct!!.startActivityForResult(cIntent, Activity.RESULT_OK)
        }
    } //endregion

    companion object {
        private const val UTYPE_PACKAGE_STATUS_NORMAL = 0
        private const val UTYPE_PACKAGE_STATUS_EMERGENCY = 1
    }

    //endregion
    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}