package com.example.mytext;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自绘大字 View，支持：
 *  - 多种字体风格（通过 setTypefaceStyle）
 *  - 静态霓虹发光（setGlow）
 *  - 彩虹渐变（setRainbow）
 *  - 颜色闪烁（setBlink）
 *  - 居中绘制 / 自动按可视区域缩放（适配远距离观看）
 *
 * 直接在 Canvas 上绘制以保证发光效果可控、效率高、可任意放缩。
 */
public class GlowTextView extends View {

    public static final int GLOW_NONE = 0;
    public static final int GLOW_SOFT = 1;     // 柔和光晕
    public static final int GLOW_STRONG = 2;   // 强烈霓虹光

    private static final int[] RAINBOW = {
            0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00,
            0xFF34C759, 0xFF00E5FF, 0xFF0A84FF,
            0xFFBF5AF2, 0xFFFF2D92
    };

    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mTextBounds = new Rect();

    private String mText = "";
    private int mTextColor = Color.WHITE;
    private int mGlowLevel = GLOW_SOFT;
    private boolean mRainbow = false;
    private boolean mBlink = false;

    /** 闪烁色相偏移（0~1） */
    private float mHueOffset = 0f;

    private ValueAnimator mRainbowAnim;
    private ValueAnimator mBlinkAnim;

    /** 字号（sp）。0 表示自动根据宽度计算 */
    private float mTextSizeSp = 0f;

    public GlowTextView(Context context) {
        super(context);
        init();
    }

    public GlowTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlowTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTextPaint.setDither(true);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        // 默认字号 120sp
        setTextSizeSp(160f);
        // 软件层（让 setShadowLayer 生效）
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setText(String text) {
        mText = text == null ? "" : text;
        requestLayout();
        invalidate();
    }

    public void setTextColorInt(int color) {
        mTextColor = color;
        mTextPaint.setColor(color);
        invalidate();
    }

    public void setGlow(int level) {
        mGlowLevel = level;
        applyShadow();
        invalidate();
    }

    public void setRainbow(boolean rainbow) {
        mRainbow = rainbow;
        if (rainbow) {
            startRainbow();
        } else {
            stopRainbow();
        }
        invalidate();
    }

    public void setBlink(boolean blink) {
        mBlink = blink;
        if (blink) {
            startBlink();
        } else {
            stopBlink();
        }
        invalidate();
    }

    /** 设置字体风格（Typeface 的 style 常量） */
    public void setTypefaceStyle(int style) {
        mTextPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, style));
        invalidate();
    }

    /** 字号 sp */
    public void setTextSizeSp(float sp) {
        mTextSizeSp = sp;
        float px = sp * getResources().getDisplayMetrics().scaledDensity;
        mTextPaint.setTextSize(px);
        requestLayout();
        invalidate();
    }

    private void applyShadow() {
        float radius;
        int color;
        switch (mGlowLevel) {
            case GLOW_SOFT:
                radius = 24f * getResources().getDisplayMetrics().density;
                color = mTextColor;
                mTextPaint.setShadowLayer(radius, 0f, 0f, color);
                break;
            case GLOW_STRONG:
                radius = 48f * getResources().getDisplayMetrics().density;
                color = mTextColor;
                mTextPaint.setShadowLayer(radius, 0f, 0f, color);
                break;
            case GLOW_NONE:
            default:
                mTextPaint.clearShadowLayer();
                break;
        }
    }

    private void startRainbow() {
        stopRainbow();
        mRainbowAnim = ValueAnimator.ofFloat(0f, 1f);
        mRainbowAnim.setDuration(3000);
        mRainbowAnim.setRepeatCount(ValueAnimator.INFINITE);
        mRainbowAnim.setRepeatMode(ValueAnimator.RESTART);
        mRainbowAnim.addUpdateListener(a -> {
            mHueOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        mRainbowAnim.start();
    }

    private void stopRainbow() {
        if (mRainbowAnim != null) {
            mRainbowAnim.cancel();
            mRainbowAnim = null;
        }
    }

    private void startBlink() {
        stopBlink();
        mBlinkAnim = ValueAnimator.ofFloat(0f, 1f);
        mBlinkAnim.setDuration(600);
        mBlinkAnim.setRepeatCount(ValueAnimator.INFINITE);
        mBlinkAnim.setRepeatMode(ValueAnimator.REVERSE);
        mBlinkAnim.addUpdateListener(a -> invalidate());
        mBlinkAnim.start();
    }

    private void stopBlink() {
        if (mBlinkAnim != null) {
            mBlinkAnim.cancel();
            mBlinkAnim = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (w <= 0) w = 1080;
        if (h <= 0) h = 1920;

        // 根据宽高自动调整字号，使文字能在屏幕里完整显示
        if (mText.length() > 0) {
            float targetPx = fitTextSize(w, h);
            mTextPaint.setTextSize(targetPx);
        }

        // 内容区域 = 整个 View
        setMeasuredDimension(w, h);
    }

    /**
     * 二分搜索一个最大字号，使得 mText 在 (w - padding) 宽度内能放下，
     * 同时不超过高度。
     */
    private float fitTextSize(int w, int h) {
        if (mText.isEmpty()) return 64f * getResources().getDisplayMetrics().scaledDensity;
        int pad = (int) (24f * getResources().getDisplayMetrics().density);
        int availW = Math.max(50, w - pad * 2);
        int availH = Math.max(50, h - pad * 2);

        float lo = 8f, hi = 600f, best = 64f;
        // 用 sp 计算最终转换为 px
        float density = getResources().getDisplayMetrics().scaledDensity;
        for (int i = 0; i < 16; i++) {
            float mid = (lo + hi) / 2f;
            mTextPaint.setTextSize(mid * density);
            mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
            if (mTextBounds.width() <= availW && mTextBounds.height() <= availH) {
                best = mid;
                lo = mid;
            } else {
                hi = mid;
            }
        }
        // 恢复（onDraw 会重新设置）
        return best * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (TextUtils.isEmpty(mText)) return;

        int w = getWidth();
        int h = getHeight();

        // 重新计算字号，确保 onMeasure 后 paint size 正确
        if (mText.length() > 0) {
            float px = fitTextSize(w, h);
            mTextPaint.setTextSize(px);
        }

        // 应用阴影
        applyShadow();

        // 处理彩虹：使用线性渐变
        if (mRainbow) {
            int[] colors = shiftRainbow();
            float[] positions = buildPositions(colors.length);
            Shader shader = new LinearGradient(
                    0, h / 2f, w, h / 2f, colors, positions, Shader.TileMode.CLAMP);
            mTextPaint.setShader(shader);
        } else {
            mTextPaint.setShader(null);
        }

        // 处理闪烁
        if (mBlink && mBlinkAnim != null) {
            float f = (float) mBlinkAnim.getAnimatedValue();
            int alpha = (int) (180 + 75 * f);
            mTextPaint.setAlpha(alpha);
        } else {
            mTextPaint.setAlpha(255);
        }

        // 计算基线
        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        float baseline = h / 2f - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;
        canvas.drawText(mText, w / 2f, baseline, mTextPaint);
    }

    private int[] shiftRainbow() {
        int n = RAINBOW.length;
        int[] out = new int[n];
        float shift = mHueOffset * n;
        for (int i = 0; i < n; i++) {
            int idx = (int) (i + shift) % n;
            if (idx < 0) idx += n;
            out[i] = RAINBOW[idx];
        }
        return out;
    }

    private float[] buildPositions(int n) {
        float[] p = new float[n];
        for (int i = 0; i < n; i++) {
            p[i] = i / (float) (n - 1);
        }
        return p;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRainbow();
        stopBlink();
    }
}
