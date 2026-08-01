package com.example.imagesecure;

public class LeaveModel {
    private String id;
    private String name;
    private String mobile;
    private String leaveDate;
    private String leaveType;
    private String reason;
    private String status;
    private String timestamp;
    private String role; // New field for role

    // Default constructor required for Firebase
    public LeaveModel() {
    }

    public LeaveModel(String id, String name, String mobile, String leaveDate, String leaveType, String reason, String status, String timestamp, String role) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.leaveDate = leaveDate;
        this.leaveType = leaveType;
        this.reason = reason;
        this.status = status;
        this.timestamp = timestamp;
        this.role = role;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getLeaveDate() {
        return leaveDate;
    }

    public void setLeaveDate(String leaveDate) {
        this.leaveDate = leaveDate;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}