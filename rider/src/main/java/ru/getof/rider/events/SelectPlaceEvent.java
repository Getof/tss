package ru.getof.rider.events;

import com.google.android.gms.maps.model.LatLng;

public class SelectPlaceEvent {
    private LatLng origin;

    public SelectPlaceEvent(LatLng origin) {
        this.origin = origin;
    }

    public LatLng getOrigin() {
        return origin;
    }

    public void setOrigin(LatLng origin) {
        this.origin = origin;
    }
}
