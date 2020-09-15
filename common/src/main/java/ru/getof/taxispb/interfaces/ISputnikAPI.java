package ru.getof.taxispb.interfaces;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ISputnikAPI {

    @GET("address")
    Call<String> geoToAddress(@Query("lat") double lat, @Query("lon") double lon,
                              @Query("count") int count);

}
