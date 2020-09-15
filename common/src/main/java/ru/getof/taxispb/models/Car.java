package ru.getof.taxispb.models;

import com.google.gson.annotations.SerializedName;

public class Car {

    @SerializedName("media")
    private Media media;

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
