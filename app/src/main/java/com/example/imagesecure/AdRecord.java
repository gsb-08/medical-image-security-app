package com.example.imagesecure;

public class AdRecord {
    public String id;
    public long createdAt;
    public String createdAtText;

    public String headline;
    public String fullText;

    public AdRecord() { }

    public AdRecord(String id, long createdAt, String createdAtText, String headline, String fullText) {
        this.id = id;
        this.createdAt = createdAt;
        this.createdAtText = createdAtText;
        this.headline = headline;
        this.fullText = fullText;
    }
}

