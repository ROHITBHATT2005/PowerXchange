package com.example.powerxchange2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GreenImpactChartView extends View {

    private Paint barPaint, barBgPaint, co2Paint, textPaint, valuePaint, emptyPaint;
    private List<SQLiteRepository.MonthlyPoint> points = new ArrayList<>();
    private boolean hasData = false;

    public GreenImpactChartView(Context c) { super(c); init(); }
    public GreenImpactChartView(Context c, AttributeSet a) { super(c, a); init(); }
    public GreenImpactChartView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#00875A"));
        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(Color.parseColor("#E6F4EA"));
        co2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        co2Paint.setColor(Color.parseColor("#4CAF50"));
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#5F6368"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#005A36"));
        valuePaint.setTextSize(24f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#5F6368"));
        emptyPaint.setTextSize(36f);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** R2-4.5: feed real data. Empty list = empty state. */
    public void setData(List<SQLiteRepository.MonthlyPoint> data) {
        points = data != null ? data : new ArrayList<>();
        hasData = false;
        for (SQLiteRepository.MonthlyPoint p : points) {
            if (p.energyKwh > 0 || p.co2Kg > 0) { hasData = true; break; }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (!hasData) {
            // R2-4.5 empty state
            canvas.drawText("No completed trades yet", w / 2f, h / 2f, emptyPaint);
            return;
        }

        float padL = 40f, padR = 40f, padT = 40f, padB = 60f;
        float cW = w - padL - padR;
        float cH = h - padT - padB;
        int n = points.size();
        if (n == 0) return;

        float maxE = 1f, maxC = 1f;
        for (SQLiteRepository.MonthlyPoint p : points) {
            if (p.energyKwh > maxE) maxE = (float) p.energyKwh;
            if (p.co2Kg > maxC) maxC = (float) p.co2Kg;
        }

        float step = cW / n;
        float barW = step * 0.30f;
        float co2W = step * 0.30f;

        for (int i = 0; i < n; i++) {
            SQLiteRepository.MonthlyPoint p = points.get(i);
            float cx = padL + (i * step) + (step / 2f);
            float bottom = h - padB;

            // Energy bar (left)
            float eLeft = cx - barW - 4f;
            float eRight = cx - 4f;
            float eTop = bottom - (cH * (float) p.energyKwh / maxE);
            RectF eBg = new RectF(eLeft, padT, eRight, bottom);
            canvas.drawRoundRect(eBg, 8f, 8f, barBgPaint);
            RectF eBar = new RectF(eLeft, eTop, eRight, bottom);
            canvas.drawRoundRect(eBar, 8f, 8f, barPaint);

            // CO2 bar (right)
            float cLeft = cx + 4f;
            float cRight = cx + co2W + 4f;
            float cTop = bottom - (cH * (float) p.co2Kg / maxC);
            RectF cBg = new RectF(cLeft, padT, cRight, bottom);
            canvas.drawRoundRect(cBg, 8f, 8f, barBgPaint);
            RectF cBar = new RectF(cLeft, cTop, cRight, bottom);
            canvas.drawRoundRect(cBar, 8f, 8f, co2Paint);

            // Month label
            canvas.drawText(p.label, cx, h - 15f, textPaint);

            // Energy value
            canvas.drawText((int) p.energyKwh + "", cx - barW / 2f - 4f,
                    Math.max(padT - 6f, eTop - 6f), valuePaint);
        }
    }
}