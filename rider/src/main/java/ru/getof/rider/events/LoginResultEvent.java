package ru.getof.rider.events;

import ru.getof.taxispb.events.BaseResultEvent;
import ru.getof.taxispb.models.Rider;

public class LoginResultEvent extends BaseResultEvent {
    public Rider rider;
    public String riderJson;
    public String jwtToken;
    public LoginResultEvent(int response, String riderJson, String jwtToken) {
        super(response);
        this.riderJson = riderJson;
        this.rider = new Rider().fromJson(riderJson);
        this.jwtToken = jwtToken;
    }
    public LoginResultEvent(int response, String message) {
        super(response,message);
    }
}
