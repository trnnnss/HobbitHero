package com.example.hobbithero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RadarChartView extends View {

    private Paint linePaint, fillPaint, textPaint;
    private int centerX, centerY;
    private int radius;
    private String[] labels = {"Spor", "Zihin", "Sosyal", "Odak", "Enerji"};
    private Map<String, Integer> data = new HashMap<>();

    public RadarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#CBD5E1"));
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#4A90E2"));
        fillPaint.setAlpha(120);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Varsayılan veriler
        data.put("Spor", 70);
        data.put("Zihin", 85);
        data.put("Sosyal", 50);
        data.put("Odak", 90);
        data.put("Enerji", 65);
    }

    public void setData(Map<String, Integer> newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = (int) (Math.min(w, h) / 2 * 0.7);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPolygons(canvas);
        drawData(canvas);
        drawLabels(canvas);
    }

    private void drawPolygons(Canvas canvas) {
        int count = labels.length;
        float angle = (float) (2 * Math.PI / count);

        // 3 iç içe poligon çiz
        for (int i = 1; i <= 3; i++) {
            float r = radius / 3 * i;
            Path path = new Path();
            for (int j = 0; j < count; j++) {
                float x = (float) (centerX + r * Math.sin(angle * j));
                float y = (float) (centerY - r * Math.cos(angle * j));
                if (j == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            canvas.drawPath(path, linePaint);
        }

        // Merkezden köşelere çizgiler
        for (int j = 0; j < count; j++) {
            float x = (float) (centerX + radius * Math.sin(angle * j));
            float y = (float) (centerY - radius * Math.cos(angle * j));
            canvas.drawLine(centerX, centerY, x, y, linePaint);
        }
    }

    private void drawData(Canvas canvas) {
        int count = labels.length;
        float angle = (float) (2 * Math.PI / count);
        Path path = new Path();

        for (int j = 0; j < count; j++) {
            int val = data.getOrDefault(labels[j], 0);
            float r = (float) radius * val / 100;
            float x = (float) (centerX + r * Math.sin(angle * j));
            float y = (float) (centerY - r * Math.cos(angle * j));
            if (j == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, fillPaint);
        
        // Kenar çizgisi
        Paint stroke = new Paint(fillPaint);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setAlpha(255);
        stroke.setStrokeWidth(4f);
        canvas.drawPath(path, stroke);
    }

    private void drawLabels(Canvas canvas) {
        int count = labels.length;
        float angle = (float) (2 * Math.PI / count);
        for (int j = 0; j < count; j++) {
            float x = (float) (centerX + (radius + 40) * Math.sin(angle * j));
            float y = (float) (centerY - (radius + 40) * Math.cos(angle * j));
            canvas.drawText(labels[j], x, y, textPaint);
        }
    }
}
