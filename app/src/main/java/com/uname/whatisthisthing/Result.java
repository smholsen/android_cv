package com.uname.whatisthisthing;

import com.google.gson.annotations.SerializedName;

public class Result {
    @SerializedName("batchcomplete")
    private String result;
    @SerializedName("query")
    public Query query;
}