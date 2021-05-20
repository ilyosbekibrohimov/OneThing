package com.weeknday.cheri;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AndroidHttpUtil
{
	public static final int	HUERROR_CONNECTION_SUCCESS				= 0;
	public static final int	HUERROR_CONNECTION_FAILED				= 1;
	public static final int	HUERROR_CONNECT_SERVER_PORT_NOT_SUPPORT	= 2;
	
	public static int	m_nLastError = HUERROR_CONNECTION_SUCCESS;

	
	public static String DownloadData(String addr)
	{
		m_nLastError = HUERROR_CONNECTION_SUCCESS;
		StringBuilder html = new StringBuilder();
		
		try
		{
			URL url = new URL(addr);
			HttpURLConnection conn = null;
			
			if(url.getProtocol().toLowerCase().equals("https"))
			{ 
                trustAllHosts(); 
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection(); 
                https.setHostnameVerifier(DO_NOT_VERIFY); 
                conn = https;
            }
			else
            	conn = (HttpURLConnection) url.openConnection();
			
			Log.i("AndroidHttpUtil", "conn : " + conn);
			
			if(conn != null)
			{
				conn.setConnectTimeout( 10000 );
				conn.setReadTimeout( 10000 );
				conn.setUseCaches(false);
				
				Log.i("AndroidHttpUtil", "getResponseCode Start");
				int resultcode = conn.getResponseCode();
				Log.i("AndroidHttpUtil", "Result Code : " + resultcode);
				
				if( conn.getResponseCode() == HttpURLConnection.HTTP_OK )
				{
					m_nLastError = HUERROR_CONNECTION_SUCCESS;
					
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					for(;;)
					{
						String line = br.readLine();
						if (line == null) break;
						html.append(line + '\n'); 
					}
					br.close();
				}
				else
					m_nLastError = HUERROR_CONNECTION_FAILED;
				
				//Log.i("AndroidHttpUtil", "Result Data : " + html);
				
				conn.disconnect();
			}
		} 
		catch (Exception ex)
		{
			m_nLastError = HUERROR_CONNECT_SERVER_PORT_NOT_SUPPORT;
			Log.i("AndroidHttpUtil", "Exception : " + m_nLastError);
			return ex.getMessage();
		}
		
		return html.toString();
	}
    
    private static void trustAllHosts()
    { 
        // Create a trust manager that does not validate certificate chains 
        TrustManager[] trustAllCerts = new TrustManager[]
        {
			new X509TrustManager()
			{ 
				public java.security.cert.X509Certificate[] getAcceptedIssuers()
		        { 
		        	return new java.security.cert.X509Certificate[] {}; 
		        } 
		 
		        @Override 
		        public void checkClientTrusted( java.security.cert.X509Certificate[] chain, String authType ) throws java.security.cert.CertificateException
		        { 
		        	// TODO Auto-generated method stub 
		        } 
		 
		        @Override 
		        public void checkServerTrusted( java.security.cert.X509Certificate[] chain, String authType ) throws java.security.cert.CertificateException
		        { 
		        	// TODO Auto-generated method stub 
		        }
			}
        }; 
 
        // Install the all-trusting trust manager 
        try
        { 
                SSLContext sc = SSLContext.getInstance("TLS"); 
                sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
        }
        catch (Exception e)
        { 
        		e.printStackTrace(); 
        } 
    }

    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier()
    { 
    	@Override
		public boolean verify(String arg0, SSLSession arg1)
    	{
			// TODO Auto-generated method stub
			return true;
		} 
    }; 
	
}