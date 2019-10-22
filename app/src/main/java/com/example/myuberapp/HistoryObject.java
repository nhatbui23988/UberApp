package com.example.myuberapp;

public class HistoryObject {
    private String historyID;
    private String driverID;
    private String customerID;
    private String comment;
    private String rating;
    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public HistoryObject(String historyID, String driverID, String customerID, String comment, String rating, String date) {
        this.historyID = historyID;
        this.driverID = driverID;
        this.customerID = customerID;
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

    public String getDriverID() {
        return driverID;
    }

    public void setDriverID(String driverID) {
        this.driverID = driverID;
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
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
