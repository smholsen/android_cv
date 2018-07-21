package com.uname.whatisthisthing;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

interface WikiService {

    @GET("?format=json&action=query&prop=extracts&exintro=&explaintext=")
    Call<com.uname.whatisthisthing.Result> search(@Query("titles") String search);
}