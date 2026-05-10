package com.example.smartparking.models;

public class ParkingSlot {
    private String slotId;
    private String status;
    private boolean isSpecialNeeds;

    public ParkingSlot() {}

    public ParkingSlot(String slotId, String status, boolean isSpecialNeeds) {
        this.slotId = slotId;
        this.status = status;
        this.isSpecialNeeds = isSpecialNeeds;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSpecialNeeds() { return isSpecialNeeds; }
    public void setSpecialNeeds(boolean specialNeeds) { this.isSpecialNeeds = specialNeeds; }
}