// Prediction.java
package com.example.camerafunction;

import com.google.gson.annotations.SerializedName;

public class Prediction {
    @SerializedName("x")
    private double x;
    @SerializedName("y")
    private double y;
    @SerializedName("width")
    private double width;
    @SerializedName("height")
    private double height;
    @SerializedName("confidence")
    private double confidence;
    @SerializedName("class")
    private String className;
    @SerializedName("class_id")
    private int classId;
    @SerializedName("detection_id")
    private String detectionId;

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getConfidence() { return confidence; }
    public String getClassName() { return className; }
    public int getClassId() { return classId; }
    public String getDetectionId() { return detectionId; }
}