package com.uname.whatisthisthing;


import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class RetrofitBuilder {
    private Result mResponse = null;

    private static final String BASE_URL = "https://en.wikipedia.org/w/api.php/";
    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private WikiService service = retrofit.create(WikiService.class);

    void performQuery(String searchTerm, final Main main){

        Call<Result> call = service.search(searchTerm);
        call.enqueue(new Callback<Result>() {

            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                mResponse = response.body();
                main.restResponseReceived(mResponse);

            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.e("Retrofit Error", t.toString());
            }
        });
    }

}
