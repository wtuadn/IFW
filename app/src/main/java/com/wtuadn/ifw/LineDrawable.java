package com.wtuadn.ifw;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

public class LineDrawable extends Drawable {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private int gravity = Gravity.BOTTOM;

    public LineDrawable(int color, float lineWidth) {
        this(color, Gravity.BOTTOM, lineWidth);
    }

    public LineDrawable(int color, int gravity, float lineWidth) {
        paint.setColor(color);
        paint.setStrokeWidth(lineWidth);
        this.gravity = gravity;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        switch (gravity) {
            case Gravity.TOP:
                float y = bounds.top + paint.getStrokeWidth() / 2f;
                canvas.drawLine(bounds.left, y, bounds.right, y, paint);
                break;
            case Gravity.BOTTOM:
                y = bounds.bottom - paint.getStrokeWidth() / 2f;
                canvas.drawLine(bounds.left, y, bounds.right, y, paint);
                break;
            case Gravity.LEFT:
                float x = bounds.left + paint.getStrokeWidth() / 2f;
                canvas.drawLine(x, bounds.top, x, bounds.bottom, paint);
                break;
            case Gravity.RIGHT:
            default:
                x = bounds.right - paint.getStrokeWidth() / 2f;
                canvas.drawLine(x, bounds.top, x, bounds.bottom, paint);
                break;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}