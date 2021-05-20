package com.weeknday.onething

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

object AndroidHttpUtil {
    const val HUERROR_CONNECTION_SUCCESS = 0
    const val HUERROR_CONNECTION_FAILED = 1
    const val HUERROR_CONNECT_SERVER_PORT_NOT_SUPPORT = 2
    var m_nLastError = HUERROR_CONNECTION_SUCCESS
    fun DownloadData(addr: String?): String? {
        m_nLastError = HUERROR_CONNECTION_SUCCESS
        val html = StringBuilder()
        try {
            val url = URL(addr)
            val conn: HttpURLConnection?
            if (url.protocol.toLowerCase() == "https") {
                trustAllHosts()
                val https = url.openConnection() as HttpsURLConnection
                https.hostnameVerifier = DO_NOT_VERIFY
                conn = https
            } else conn = url.openConnection() as HttpURLConnection
            Log.i("AndroidHttpUtil", "conn : $conn")
            if (conn != null) {
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.useCaches = false
                Log.i("AndroidHttpUtil", "getResponseCode Start")
                val resultcode = conn.responseCode
                Log.i("AndroidHttpUtil", "Result Code : $resultcode")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    m_nLastError = HUERROR_CONNECTION_SUCCESS
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    while (true) {
                        val line = br.readLine() ?: break
                        html.append(line).append('\n')
                    }
                    br.close()
                } else m_nLastError = HUERROR_CONNECTION_FAILED

                //Log.i("AndroidHttpUtil", "Result Data : " + html);
                conn.disconnect()
            }
        } catch (ex: Exception) {
            m_nLastError = HUERROR_CONNECT_SERVER_PORT_NOT_SUPPORT
            Log.i("AndroidHttpUtil", "Exception : " + m_nLastError)
            return ex.message
        }
        return html.toString()
    }

    private fun trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    // TODO Auto-generated method stub
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    // TODO Auto-generated method stub
                }
            }
        )

        // Install the all-trusting trust manager
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val DO_NOT_VERIFY =
        HostnameVerifier { arg0, arg1 -> // TODO Auto-generated method stub
            true
        }
}