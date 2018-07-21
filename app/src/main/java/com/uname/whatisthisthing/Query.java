package com.uname.whatisthisthing;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Query {
    @SerializedName("pages")
    public Map<String, Page> pages;

    public void setPages(Map<String, Page> pages) {
        this.pages = pages;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}