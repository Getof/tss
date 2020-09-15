package ru.getof.taxispb.utils;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Retrofit instance;

    public static Retrofit getInstance(){
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Authorization", "Token 289cb36ec9fe6a88e2dc1d46cee06d9cca18b4f1")
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        OkHttpClient client = httpClient.build();

        return instance == null ? new Retrofit.Builder()
                .baseUrl("https://suggestions.dadata.ru/suggestions/api/4_1/rs/geolocate/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client)
                .build() : instance;
    }
}
