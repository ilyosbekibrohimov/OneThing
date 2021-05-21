package com.weeknday.onething

import android.app.Application
import com.kakao.auth.KakaoSDK


class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        mInstance = this
        KakaoSDK.init(KakaoSDKAdapter())
    }

    companion object {
        private var mInstance: GlobalApplication? = null
        val globalApplicationContext: GlobalApplication?
            get() {
                checkNotNull(mInstance) { "this application does not inherit GlobalApplication" }
                return mInstance
            }
    }
}
