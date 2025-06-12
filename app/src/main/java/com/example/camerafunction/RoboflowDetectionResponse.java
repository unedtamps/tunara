package com.example.camerafunction;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoboflowDetectionResponse {
    @SerializedName("inference_id")
    private String inferenceId;
    @SerializedName("time")
    private double time;
    @SerializedName("image")
    private ImageInfo image;
    @SerializedName("predictions")
    private List<Prediction> predictions;

    // Getters
    public String getInferenceId() { return inferenceId; }
    public double getTime() { return time; }
    public ImageInfo getImage() { return image; }
    public List<Prediction> getPredictions() { return predictions; }

    public static class ImageInfo {
        @SerializedName("width")
        private int width;
        @SerializedName("height")
        private int height;

        // Getters
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
}