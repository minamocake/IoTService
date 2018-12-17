package com.sap.digitallab.iotservice.util;

import com.google.gson.JsonObject;
import com.sap.digitallab.iotservice.Constants;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiHelper {

    @Headers("Content-Type: application/json")
    @POST(Constants.URL_IOT)
    Call<ResponseBody> sendLocation(@Body JsonObject jsonBody);

}
