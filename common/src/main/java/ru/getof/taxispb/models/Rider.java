package ru.getof.taxispb.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import static ru.getof.taxispb.BR.*;
import static ru.getof.taxispb.BR.*;

public class Rider extends BaseObservable {

    @Expose
    @SerializedName("first_name")
    public String firstName;

    @Expose
    @SerializedName("last_name")
    public String lastName;

    @Expose
    @SerializedName("mobile_number")
    public long mobileNumber;

    public String status;

    @Expose
    public String email;

    @Expose
    public String address;

    public static Rider fromJson (String json) {
        return (new GsonBuilder().create().fromJson(json, Rider.class));
    }

    public static String toJson (Rider rider) {
        return (new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(rider));
    }

   public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;


    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;


    }


    public long getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(long mobileNumber) {
        this.mobileNumber = mobileNumber;

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;


    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;

    }
}
