package ru.getof.taxispb.utils;

public enum ServerResponse {
    REQUEST_TIMEOUT(408),
    OK(200),
    UNKNOWN_ERROR(666),
    PAYMENT_ERROR(400),
    NOT_FOUND(404),
    UPDATE_APP(410),
    NO_CLOSE_FOUND(303),
    HAS_PAYMENT_REQUEST(901),
    NO_SUFFICIENT_AMOUNT(902),
    NO_SUFFICIENT_CREDIT(903);
    private int value;
    ServerResponse(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    public static ServerResponse get(int code) {
        for(ServerResponse s : values()) {
            if(s.value == code) return s;
        }
        return UNKNOWN_ERROR;
    }
}
