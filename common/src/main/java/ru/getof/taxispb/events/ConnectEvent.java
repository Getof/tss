package ru.getof.taxispb.events;

import ru.getof.taxispb.utils.ServerResponse;

public class ConnectEvent extends BaseRequestEvent {
    public String token;
    public ConnectEvent(String token) {
        super (new ConnectResultEvent(ServerResponse.REQUEST_TIMEOUT.getValue()));
        this.token = token;
    }
}
