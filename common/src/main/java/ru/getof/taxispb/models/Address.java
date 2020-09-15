package ru.getof.taxispb.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Address implements Serializable {

    @SerializedName("address")
    private String address;

    @SerializedName("location")
    private LatLng location;

    @SerializedName("region")
    private String region;

    @SerializedName("city")
    private String city;



}
