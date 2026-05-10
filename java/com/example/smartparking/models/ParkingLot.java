package com.example.smartparking.models;

public class ParkingLot {
    private String id;
    private String title;
    private String category;
    private int availableCount;
    private int specialNeedsCount;
    private String price;
    private String distance;
    private int gradientResId;
    private String imageUrl;

    public ParkingLot() {

    }


    public ParkingLot(String title, String category, int availableCount, int specialNeedsCount, String price, String distance, int gradientResId, String imageUrl) {
        this.title = title;
        this.category = category;
        this.availableCount = availableCount;
        this.specialNeedsCount = specialNeedsCount;
        this.price = price;
        this.distance = distance;
        this.gradientResId = gradientResId;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getAvailableCount() { return availableCount; }
    public int getSpecialNeedsCount() { return specialNeedsCount; }
    public String getPrice() { return price; }
    public String getDistance() { return distance; }
    public int getGradientResId() { return gradientResId; }
    public String getImageUrl() { return imageUrl; }
}