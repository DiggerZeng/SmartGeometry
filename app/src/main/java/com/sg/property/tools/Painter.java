package com.sg.property.tools;

import android.graphics.Paint;

public class Painter {

    private Paint paint;
    private int color;
    private float width;

    public Painter(int color, float width) {
        this.paint = new Paint();
        this.color = color;
        this.width = width;
        initPainter();
    }


    public Painter(Paint paint) {
        this.paint = paint;
    }

    private void initPainter() {
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
    }

    public void setColor(int color) {
        this.color = color;
        paint.setColor(color);
    }

    public int getColor(){
        return color;
    }

    public void setWidth(float width) {
        this.width = width;
        paint.setStrokeWidth(width);
    }

    public float getWidth() {
        return width;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public Paint getPaint() {
        return paint;
    }

}
