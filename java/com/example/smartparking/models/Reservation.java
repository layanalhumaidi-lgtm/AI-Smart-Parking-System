package com.example.smartparking.models;

public class Reservation {
    private String id;
    private String parkingName;
    private String plateNumber;
    private String spotId;
    private String duration;
    private String totalPrice;
    private String status;
    private String date;
    private String startTime;
    private String endTime;
    private long startTimestamp;
    private long endTimestamp;

    public Reservation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getParkingName() {
        return parkingName;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getDuration() {
        return duration;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}