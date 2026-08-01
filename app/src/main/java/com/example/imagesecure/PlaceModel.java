package com.example.imagesecure;

public class PlaceModel {
    public String name, type;
    public double lat, lng;

    public PlaceModel(String name, String type, double lat, double lng) {
        this.name = name;
        this.type = type;
        this.lat = lat;
        this.lng = lng;
    }
}

