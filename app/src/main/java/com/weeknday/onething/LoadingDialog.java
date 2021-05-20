package com.weeknday.onething;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;


public class LoadingDialog extends Dialog
{
	private ImageView		m_ivImage = null;
    private AnimationDrawable m_cAnim = null;

    public LoadingDialog(@NonNull Context context)
    {
        super(context);

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void InitCtrl()
    {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        super.onCreate(savedInstanceState);
        setContentView(com.weeknday.onething.R.layout.dlg_loading);

        InitCtrl();

        m_ivImage.setVisibility(View.VISIBLE);
        m_ivImage.setBackgroundResource(com.weeknday.onething.R.drawable.loading_animation);

        m_ivImage.setBackgroundColor(Color.TRANSPARENT);
        m_cAnim = (AnimationDrawable) m_ivImage.getDrawable();

    }

    @Override
    public void show()
    {
        super.show();

        m_cAnim.start();
    }

}