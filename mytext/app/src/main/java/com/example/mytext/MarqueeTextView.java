package com.example.mytext;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * 自定义滚动文字 View。
 *
 * 支持 4 个方向滚动：左、右、上、下。
 * 文字以霓虹发光风格绘制，根据屏幕尺寸自适应字号。
 * 通过 ValueAnimator 驱动 offset 重绘，避免依赖系统 marquee。
 */
public class MarqueeTextView extends View {

    public static final int DIR_LEFT = 0;
    public static final int DIR_RIGHT = 1;
    public static final int DIR_UP = 2;
    public static final int DIR_DOWN = 3;

    private final TextPaint mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mBounds = new Rect();

    private String mText = "";
    private int mTextColor = Color.WHITE;
    private int mDirection = DIR_LEFT;
    private long mDurationMs = 8000;

    private ValueAnimator mAnimator;
    private float mOffset = 0f;

    public MarqueeTextView(Context context) {
        super(context);
        init();
    }

    public MarqueeTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setDither(true);
        mPaint.setFakeBoldText(true);
        mPaint.setColor(mTextColor);
        mPaint.setTextAlign(Paint.Align.LEFT);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // 让 setShadowLayer 生效
        float density = getResources().getDisplayMetrics().scaledDensity;
        mPaint.setTextSize(160f * density);
        applyGlow();
    }

    public void setText(String text) {
        mText = text == null ? "" : text;
        requestLayout();
        invalidate();
    }

    public void setTextColorInt(int color) {
        mTextColor = color;
        mPaint.setColor(color);
        applyGlow();
        invalidate();
    }

    public void setTypefaceStyle(int style) {
        mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        invalidate();
    }

    public void setDirection(int dir) {
        mDirection = dir;
        invalidate();
    }

    public void setDurationMs(long ms) {
        mDurationMs = ms;
        if (mAnimator != null) {
            mAnimator.setDuration(ms);
        }
    }

    private void applyGlow() {
        float density = getResources().getDisplayMetrics().density;
        mPaint.setShadowLayer(28f * density, 0f, 0f, mTextColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (w <= 0) w = 1080;
        if (h <= 0) h = 1920;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 根据文字长度动态调整时长，让长字也能看清
        long base = 5000L;
        float lenFactor = Math.max(1f, mText.length() / 4f);
        setDurationMs((long) (base * lenFactor));
        startScroll();
    }

    private void startScroll() {
        stopScroll();
        if (mText.isEmpty()) return;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mDurationMs);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(a -> {
            mOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    private void stopScroll() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mText.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        float density = getResources().getDisplayMetrics().scaledDensity;

        // 计算字号：横向滚动按高度走，纵向滚动按宽度走
        int pad = (int) (24f * density);
        if (mDirection == DIR_LEFT || mDirection == DIR_RIGHT) {
            float target = fitByHeight(h - pad * 2);
            mPaint.setTextSize(target);
        } else {
            float target = fitByWidth(w - pad * 2);
            mPaint.setTextSize(target);
        }
        applyGlow();

        mPaint.getTextBounds(mText, 0, mText.length(), mBounds);
        float textW = mBounds.width();
        float textH = mBounds.height();

        float x, y;
        float baseline = h / 2f - (mPaint.descent() + mPaint.ascent()) / 2f;

        switch (mDirection) {
            case DIR_LEFT:
                // 从右屏外 -> 左屏外（文字向左移动）
                // mOffset=0 时 x=w（文字左缘贴屏幕右边，刚好开始进入）
                // mOffset=1 时 x=-textW（文字右缘贴屏幕左边，刚好完全离开）
                x = w - (w + textW) * mOffset;
                y = baseline;
                break;
            case DIR_RIGHT:
                // 从左屏外 -> 右屏外（文字向右移动）
                // mOffset=0 时 x=-textW（文字右缘贴屏幕左边）
                // mOffset=1 时 x=w（文字左缘贴屏幕右边）
                x = -textW + (w + textW) * mOffset;
                y = baseline;
                break;
            case DIR_UP:
                // 从下屏外 -> 上屏外（文字向上移动）
                // mOffset=0 时 baseline=h+textH（文字顶部贴屏幕底部）
                // mOffset=1 时 baseline=-textH（文字底部贴屏幕顶部之上）
                x = (w - textW) / 2f;
                y = (h + textH) - (h + textH) * mOffset;
                break;
            case DIR_DOWN:
            default:
                // 从上屏外 -> 下屏外（文字向下移动）
                x = (w - textW) / 2f;
                y = -textH + (h + textH) * mOffset;
                break;
        }

        canvas.drawText(mText, x, y, mPaint);
    }

    private float fitByHeight(int availH) {
        if (mText.isEmpty()) return 64f;
        float density = getResources().getDisplayMetrics().scaledDensity;
        float lo = 8f, hi = 600f, best = 80f;
        for (int i = 0; i < 16; i++) {
            float mid = (lo + hi) / 2f;
            mPaint.setTextSize(mid * density);
            mPaint.getTextBounds(mText, 0, mText.length(), mBounds);
            if (mBounds.height() <= availH) {
                best = mid;
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return best * density;
    }

    private float fitByWidth(int availW) {
        if (mText.isEmpty()) return 64f;
        float density = getResources().getDisplayMetrics().scaledDensity;
        float lo = 8f, hi = 600f, best = 80f;
        for (int i = 0; i < 16; i++) {
            float mid = (lo + hi) / 2f;
            mPaint.setTextSize(mid * density);
            mPaint.getTextBounds(mText, 0, mText.length(), mBounds);
            if (mBounds.width() <= availW) {
                best = mid;
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return best * density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopScroll();
    }
}
