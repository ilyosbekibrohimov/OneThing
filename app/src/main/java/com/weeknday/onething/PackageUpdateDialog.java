package com.weeknday.onething;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PackageUpdateDialog extends Dialog {

    //region vars
    private String m_strAction = "";
    private static final int UTYPE_PACKAGE_STATUS_NORMAL = 0;
    private static final int UTYPE_PACKAGE_STATUS_EMERGENCY = 1;
    private int m_nStatus = UTYPE_PACKAGE_STATUS_NORMAL;
    private boolean m_bIsInit = false;

    private LinearLayout m_lyContents = null;
    private LinearLayout m_lyBtnYN = null;
    private LinearLayout m_lyBtnConfirm = null;
    private Button m_btnYes = null;
    private Button m_btnNo = null;
    private Button m_btnConfirm = null;
    private TextView m_tvUptContents = null;

    private final Activity m_cAct;
    private final Handler m_cDlgHandler = new Handler();
    //endregion


    public PackageUpdateDialog(@NonNull Context context, Activity cAct) {
        super(context);
        m_cAct = cAct;
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }




    //region overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.weeknday.onething.R.layout.dlg_pkgupdate);

        if (m_bIsInit)
            return;

        m_lyContents = (LinearLayout) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_ly_contents);
        m_lyBtnYN = (LinearLayout) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_ly_btn_yn);
        m_lyBtnConfirm = (LinearLayout) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_ly_btn_confirm);

        m_btnYes = (Button) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_btn_y);
        m_btnNo = (Button) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_btn_n);
        m_btnConfirm = (Button) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_btn_confirm);

        m_tvUptContents = (TextView) findViewById(com.weeknday.onething.R.id.dlg_pkgupt_tv_update_contents);

        SetBtnListener();

        SetDialogControls();

        m_bIsInit = true;
    }
    //endregion
    //region functions
    private void SetBtnListener() {
        m_btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunAction();
                dismiss();

                if (m_cAct != null)
                    m_cAct.finish();
            }
        });

        m_btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        m_btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunAction();
                dismiss();

                if (m_cAct != null)
                    m_cAct.finish();
            }
        });
    }
    public void setStatus(int nLevel, String strAction) {
        switch (nLevel) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                m_nStatus = UTYPE_PACKAGE_STATUS_NORMAL;
                break;
            case 7:
            case 8:
            case 9:
                m_nStatus = UTYPE_PACKAGE_STATUS_EMERGENCY;
                break;
        }

        m_strAction = strAction;

        SetDialogControls();
    }

    public void SetDialogControls() {
        ShowContents();

        switch (m_nStatus) {
            case UTYPE_PACKAGE_STATUS_NORMAL:
                ShowBtnGroupYN(View.VISIBLE);
                ShowBtnGroupConfirm(View.GONE);

                if (m_tvUptContents != null)
                    m_tvUptContents.setText(com.weeknday.onething.R.string.app_update_contents);
                break;

            case UTYPE_PACKAGE_STATUS_EMERGENCY:
                ShowBtnGroupYN(View.GONE);
                ShowBtnGroupConfirm(View.VISIBLE);

                if (m_tvUptContents != null)
                    m_tvUptContents.setText(com.weeknday.onething.R.string.app_emergency_update_contents);
                break;
        }
    }

    private void ShowContents() {
        if (m_lyContents != null)
            m_lyContents.setVisibility(View.VISIBLE);
    }

    private void ShowBtnGroupYN(int nVisibility) {
        if (m_lyBtnYN != null)
            m_lyBtnYN.setVisibility(nVisibility);
    }

    private void ShowBtnGroupConfirm(int nVisibility) {
        if (m_lyBtnConfirm != null)
            m_lyBtnConfirm.setVisibility(nVisibility);
    }

    private void RunAction() {
        m_cDlgHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent cIntent = new Intent();
                cIntent.setAction(Intent.ACTION_VIEW);
                cIntent.setData(Uri.parse(m_strAction));
                m_cAct.startActivityForResult(cIntent, Activity.RESULT_OK);
            }
        });
    }
    //endregion
}