package org.pytorch.demo.objectdetection;

import android.graphics.Bitmap;

public class ImageTextData {
    private Bitmap image;
    private String headText;
    private String upText;
    private String lowText;

    public ImageTextData(Bitmap image, String headText, String upText, String lowText) {
        this.image = image;
        this.headText = headText;
        this.upText = upText;
        this.lowText = lowText;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getHeadText() {
        return headText;
    }

    public String getUpText() {
        return upText;
    }

    public String getLowText() {
        return lowText;
    }
}
