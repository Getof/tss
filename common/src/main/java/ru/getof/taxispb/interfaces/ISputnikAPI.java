package ru.getof.taxispb.interfaces;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ISputnikAPI {

    @POST("address")
    Call<String> geoToAddress(@Query("lat") double lat, @Query("lon") double lon);

}
