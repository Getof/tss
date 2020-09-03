package ru.getof.taxispb.events;

public class LoginEvent {
    public long userName;
    public int versionNumber;
    public LoginEvent(long userName,int versionNumber){
        this.userName = userName;
        this.versionNumber = versionNumber;
    }
}
