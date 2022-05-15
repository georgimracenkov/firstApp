package com.example.kursov.models;

import java.io.Serializable;
import java.util.List;

public class Information implements Serializable {
    private String title;
    private String informationText;
    private List<String> imageUrls;

    public Information() {

    }

    public Information(String title, String informationText, List<String> imageUrls) {
        this.title = title;
        this.informationText = informationText;
        this.imageUrls = imageUrls;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInformationText() {
        return informationText;
    }

    public void setInformationText(String informationText) {
        this.informationText = informationText;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
