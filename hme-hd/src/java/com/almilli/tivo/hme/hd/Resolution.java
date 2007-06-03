package com.almilli.tivo.hme.hd;

import java.io.Serializable;

public class Resolution implements Serializable {
    private static final long serialVersionUID = -8962622382688635426L;
    private int width;
    private int height;
    private int aspectNumerator;
    private int aspectDenominator;

    public Resolution(int width, int height, int aspectNumerator, int aspectDenominator) {
        this.width = width;
        this.height = height;
        this.aspectNumerator = aspectNumerator;
        this.aspectDenominator = aspectDenominator;
        
        if (aspectDenominator == 0) {
            throw new IllegalArgumentException("The aspect denominator cannot be zero.");
        }
    }

    public int getHeight() {
        return height;
    }

    public int getPixelAspectDenominator() {
        return aspectDenominator;
    }

    public int getPixelAspectNumerator() {
        return aspectNumerator;
    }

    public int getSafeActionHorizontal() {
        return (width * 5) / 100;
    }

    public int getSafeActionVertical() {
        return (height * 375) / 10000;
    }

    public int getSafeTitleHorizontal() {
        return 2 * getSafeActionHorizontal();
    }

    public int getSafeTitleVertical() {
        return 2 * getSafeActionVertical();
    }

    public int getWidth() {
        return width;
    }

    public float getAspectRatio() {
        return (float)aspectNumerator / (float)aspectDenominator;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Resolution)) {
            return false;
        }
        Resolution res = (Resolution)obj;
        return res.width == width && res.height == height &&
            res.aspectNumerator == aspectNumerator && 
            res.aspectDenominator == aspectDenominator;
    }
    
    @Override
    public int hashCode() {
        int code = width;
        code ^= height;
        code ^= aspectNumerator;
        code ^= aspectDenominator;
        return code;
    }

    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Resolution[width=").append(width);
        sb.append(",height=").append(height);
        sb.append(",aspectNumerator=").append(aspectNumerator);
        sb.append(",aspectDenominator=").append(aspectDenominator);
        sb.append("]");
        return sb.toString();
    }

}
