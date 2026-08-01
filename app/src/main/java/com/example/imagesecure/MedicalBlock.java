package com.example.imagesecure;

public class MedicalBlock {

    public String doctorUid;
    public String nodeAddress;
    public String prevNodeAddress;
    public String nextNodeAddress;
    public String prevHash;
    public String currentHash;

    public String patientName;
    public String age;
    public String symptoms;
    public String medicine;
    public String imageUrl;
    public String timestamp;

    public boolean verified;

    public MedicalBlock() {
    }

    public MedicalBlock(String doctorUid,
                        String nodeAddress,
                        String prevNodeAddress,
                        String nextNodeAddress,
                        String prevHash,
                        String currentHash,
                        String patientName,
                        String age,
                        String symptoms,
                        String medicine,
                        String imageUrl,
                        String timestamp,
                        boolean verified) {
        this.doctorUid = doctorUid;
        this.nodeAddress = nodeAddress;
        this.prevNodeAddress = prevNodeAddress;
        this.nextNodeAddress = nextNodeAddress;
        this.prevHash = prevHash;
        this.currentHash = currentHash;
        this.patientName = patientName;
        this.age = age;
        this.symptoms = symptoms;
        this.medicine = medicine;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.verified = verified;
    }
}