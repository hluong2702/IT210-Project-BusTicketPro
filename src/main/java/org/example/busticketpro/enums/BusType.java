package org.example.busticketpro.enums;

public enum BusType {
    STANDARD_29("Xe 29 chỗ", 29),
    STANDARD_45("Xe 45 chỗ", 45),
    SLEEPER_34("Xe giường nằm 34 chỗ", 34),
    LIMOUSINE_20("Xe Limousine 20 chỗ", 20),
    DOUBLE_DECKER_40("Xe 2 tầng 40 chỗ", 40);

    private final String displayName;
    private final int seatCount;

    BusType(String displayName, int seatCount) {
        this.displayName = displayName;
        this.seatCount = seatCount;
    }

    public String getDisplayName() { return displayName; }
    public int getSeatCount() { return seatCount; }
}
