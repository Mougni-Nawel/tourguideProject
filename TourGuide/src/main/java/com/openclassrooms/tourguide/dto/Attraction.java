package com.openclassrooms.tourguide.dto;

public class Attraction {

    private String nameAttraction;

    private Double latAttraction;
    private Double longAttraction;
    private Double latUser;
    private Double longUser;

    private Double distance;

    private int rewardPoint;

    public String getNameAttraction() {
        return nameAttraction;
    }

    public void setNameAttraction(String nameAttraction) {
        this.nameAttraction = nameAttraction;
    }

    public Double getLatAttraction() {
        return latAttraction;
    }

    public void setLatAttraction(Double latAttraction) {
        this.latAttraction = latAttraction;
    }

    public Double getLongAttraction() {
        return longAttraction;
    }

    public void setLongAttraction(Double longAttraction) {
        this.longAttraction = longAttraction;
    }

    public Double getLatUser() {
        return latUser;
    }

    public void setLatUser(Double latUser) {
        this.latUser = latUser;
    }

    public Double getLongUser() {
        return longUser;
    }

    public void setLongUser(Double longUser) {
        this.longUser = longUser;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getRewardPoint() {
        return rewardPoint;
    }

    public void setRewardPoint(int rewardPoint) {
        this.rewardPoint = rewardPoint;
    }

//    public Attraction(Attraction attraction) {
//        this.nameAttraction = attraction.getNameAttraction();
//        this.latAttraction = attraction.getLatAttraction();
//        this.longAttraction = attraction.getLongAttraction();
//    }
}
