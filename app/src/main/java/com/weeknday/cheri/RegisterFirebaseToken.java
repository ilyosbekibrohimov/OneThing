package com.weeknday.cheri;


import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterFirebaseToken
{
    public void SendRegistrationToServer(String token)
    {
        // Add custom implementation, as needed.
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("Token", token)
                .build();

        //request
        Request request = new Request.Builder()
                .url("http://push.opoksoft.com:8001/register.php")
                .post(body)
                .build();

        try
        {
            Response response = client.newCall(request).execute();
            //Log.d("TSIM", "SendRegistrationToServer Response: " + response.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
