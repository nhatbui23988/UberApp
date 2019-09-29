package com.example.myuberapp;

public class HistoryObject {
    private String historyID;
    private String driverName;
    private String customerName;
    private String comment;
    private String rating;
    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public HistoryObject(String historyID, String driverName, String customerName, String comment, String rating, String date) {
        this.historyID = historyID;
        this.driverName = driverName;
        this.customerName = customerName;
        this.comment = comment;
        this.rating = rating;
        this.date = date;
    }

    public HistoryObject(){

    }

    public String getHistoryID() {
        return historyID;
    }

    public void setHistoryID(String historyID) {
        this.historyID = historyID;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

}
