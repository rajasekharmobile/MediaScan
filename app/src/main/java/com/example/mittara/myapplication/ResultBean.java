package com.example.mittara.myapplication;

import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mittara on 6/30/2016.
 */
public class ResultBean implements Serializable{

    //stores the latest results
    LinkedHashMap<String,String> mTopFilesAndSizes = new LinkedHashMap<>();
    String mAvgFileSize = null;
    LinkedHashMap<String,String> mFrequentFileTypes = new LinkedHashMap<>();

    public LinkedHashMap<String, String> getmTopFilesAndSizes() {
        return mTopFilesAndSizes;
    }

    public void setmTopFilesAndSizes(LinkedHashMap<String, String> mTopFilesAndSizes) {
        this.mTopFilesAndSizes = mTopFilesAndSizes;
    }

    public String getmAvgFileSize() {
        return mAvgFileSize;
    }

    public void setmAvgFileSize(String mAvgFileSize) {
        this.mAvgFileSize = mAvgFileSize;
    }

    public LinkedHashMap<String, String> getmFrequentFileTypes() {
        return mFrequentFileTypes;
    }

    public void setmFrequentFileTypes(LinkedHashMap<String, String> mFrequentFileTypes) {
        this.mFrequentFileTypes = mFrequentFileTypes;
    }


}
