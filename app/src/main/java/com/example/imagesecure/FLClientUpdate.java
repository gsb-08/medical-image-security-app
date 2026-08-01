package com.example.imagesecure;

import java.util.Map;

public class FLClientUpdate {
    public String doctorUid;
    public double localAccuracy;
    public double localLoss;
    public Map<String, Double> weights;
    public long timestamp;

    public FLClientUpdate() {
    }

    public FLClientUpdate(String doctorUid, double localAccuracy, double localLoss,
                          Map<String, Double> weights, long timestamp) {
        this.doctorUid = doctorUid;
        this.localAccuracy = localAccuracy;
        this.localLoss = localLoss;
        this.weights = weights;
        this.timestamp = timestamp;
    }
}