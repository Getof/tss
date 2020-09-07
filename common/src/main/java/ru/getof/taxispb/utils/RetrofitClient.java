package ru.getof.taxispb.utils;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Retrofit instance;

    public static Retrofit getInstance(){
        return instance == null ? new Retrofit.Builder()
                .baseUrl("http://89.145.146.20:3030/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build() : instance;
    }
}
