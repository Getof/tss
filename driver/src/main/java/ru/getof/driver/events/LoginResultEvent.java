package ru.getof.driver.events;

import com.google.gson.Gson;

import ru.getof.taxispb.events.BaseResultEvent;
import ru.getof.taxispb.models.Driver;

public class LoginResultEvent extends BaseResultEvent {
    public Driver driver;
    public String driverJson;
    public String jwtToken;
    public LoginResultEvent(int response, String driverJson, String jwtToken) {
        super(response);
        this.driverJson = driverJson;
        this.driver = new Gson().fromJson(driverJson,Driver.class);
        this.jwtToken = jwtToken;
    }
    public LoginResultEvent(int response, String message) {
        super(response,message);
    }
}
