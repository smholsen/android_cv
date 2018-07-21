package com.uname.whatisthisthing;

import com.google.gson.annotations.SerializedName;

public class Page {
    @SerializedName("pageid")
    private long id;
    @SerializedName("title")
    public String title;
    @SerializedName("extract")
    public String content;
}