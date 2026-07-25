package com.example.mytext;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * 全屏展示页面。
 *
 * 接收 MainActivity 传入的参数，根据所选特效构造对应视图并展示。
 * 同时启动 TTS 朗读（如启用）和背景音乐（如选择）。
 */
public class DisplayActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "DisplayActivity";

    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_FONT_INDEX = "extra_font_index";
    public static final String EXTRA_EFFECT_INDEX = "extra_effect_index";
    public static final String EXTRA_COLOR_INDEX = "extra_color_index";
    public static final String EXTRA_BG_COLOR_INDEX = "extra_bg_color_index";
    public static final String EXTRA_TEXT_SIZE = "extra_text_size";
    public static final String EXTRA_TTS_ENABLED = "extra_tts_enabled";
    public static final String EXTRA_TTS_LOOP = "extra_tts_loop";
    public static final String EXTRA_MUSIC_INDEX = "extra_music_index";

    // 效果索引（与 strings.xml 中 effect_styles 对应）
    private static final int EFFECT_GLOW_STATIC = 0;
    private static final int EFFECT_PULSE = 1;
    private static final int EFFECT_NEON_BLINK = 2;
    private static final int EFFECT_RAINBOW = 3;
    private static final int EFFECT_SCROLL_LEFT = 4;
    private static final int EFFECT_SCROLL_RIGHT = 5;
    private static final int EFFECT_SCROLL_UP = 6;
    private static final int EFFECT_SCROLL_DOWN = 7;
    private static final int EFFECT_BOUNCE = 8;

    // 字体索引（与 strings.xml 中 font_styles 对应）
    private static final int FONT_DEFAULT = 0;
    private static final int FONT_BOLD = 1;
    private static final int FONT_SERIF = 2;
    private static final int FONT_MONOSPACE = 3;
    private static final int FONT_ITALIC = 4;
    private static final int FONT_BOLD_ITALIC = 5;

    private FrameLayout mTextContainer;
    private View mActiveView;

    private TextToSpeech mTts;
    private boolean mTtsReady = false;
    private boolean mTtsEnabled;
    private boolean mTtsLoop;
    private String mDisplayText;

    private ToneMusicPlayer mMusicPlayer;
    private int mMusicIndex;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private GestureDetector mGestureDetector;

    // TTS 朗读完成回调
    private final UtteranceProgressListenerImpl mUtteranceListener =
            new UtteranceProgressListenerImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏 / 导航栏，进入沉浸式
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        setContentView(R.layout.activity_display);

        mTextContainer = findViewById(R.id.textContainer);

        Intent intent = getIntent();
        mDisplayText = intent.getStringExtra(EXTRA_TEXT);
        if (mDisplayText == null || mDisplayText.trim().isEmpty()) {
            mDisplayText = "MyText";
        }
        int fontIndex = intent.getIntExtra(EXTRA_FONT_INDEX, 0);
        int effectIndex = intent.getIntExtra(EXTRA_EFFECT_INDEX, 0);
        int colorIndex = intent.getIntExtra(EXTRA_COLOR_INDEX, 0);
        int bgColorIndex = intent.getIntExtra(EXTRA_BG_COLOR_INDEX, 0);
        int musicIndex = intent.getIntExtra(EXTRA_MUSIC_INDEX, 0);
        mMusicIndex = musicIndex;
        mTtsEnabled = intent.getBooleanExtra(EXTRA_TTS_ENABLED, true);
        mTtsLoop = intent.getBooleanExtra(EXTRA_TTS_LOOP, false);

        int textColor = getResources().getIntArray(R.array.text_colors)[colorIndex];
        int bgColor = getResources().getIntArray(R.array.bg_colors)[bgColorIndex];
        mTextContainer.setBackgroundColor(bgColor);

        buildActiveView(effectIndex, fontIndex, textColor);

        // 单击退出，双击切换横竖屏（保持竖屏不旋转）
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                finish();
                return true;
            }
        });

        // 音乐
        if (mMusicIndex > 0) {
            mMusicPlayer = new ToneMusicPlayer();
            mMusicPlayer.start(mMusicIndex - 1);
        }

        // TTS 初始化
        if (mTtsEnabled) {
            mTts = new TextToSpeech(this, this);
        }
    }

    private void hideSystemUi() {
        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(flags);
        decor.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decor.setSystemUiVisibility(flags);
            }
        });
    }

    private void buildActiveView(int effectIndex, int fontIndex, int textColor) {
        mTextContainer.removeAllViews();
        int typefaceStyle = mapFontStyle(fontIndex);

        // 滚动类使用 MarqueeTextView；其它用 GlowTextView
        switch (effectIndex) {
            case EFFECT_SCROLL_LEFT:
            case EFFECT_SCROLL_RIGHT:
            case EFFECT_SCROLL_UP:
            case EFFECT_SCROLL_DOWN: {
                MarqueeTextView mv = new MarqueeTextView(this);
                mv.setText(mDisplayText);
                mv.setTextColorInt(textColor);
                mv.setTypefaceStyle(typefaceStyle);
                int dir;
                switch (effectIndex) {
                    case EFFECT_SCROLL_RIGHT: dir = MarqueeTextView.DIR_RIGHT; break;
                    case EFFECT_SCROLL_UP:    dir = MarqueeTextView.DIR_UP;    break;
                    case EFFECT_SCROLL_DOWN:  dir = MarqueeTextView.DIR_DOWN;  break;
                    case EFFECT_SCROLL_LEFT:
                    default:                  dir = MarqueeTextView.DIR_LEFT;   break;
                }
                mv.setDirection(dir);
                mActiveView = mv;
                break;
            }
            case EFFECT_PULSE:
            case EFFECT_NEON_BLINK:
            case EFFECT_RAINBOW:
            case EFFECT_BOUNCE:
            case EFFECT_GLOW_STATIC:
            default: {
                final GlowTextView gv = new GlowTextView(this);
                gv.setText(mDisplayText);
                gv.setTextColorInt(textColor);
                gv.setTypefaceStyle(typefaceStyle);

                switch (effectIndex) {
                    case EFFECT_PULSE:
                        gv.setGlow(GlowTextView.GLOW_SOFT);
                        gv.post(() -> {
                            ObjectAnimator a = ObjectAnimator.ofFloat(gv, "scaleX", 0.9f, 1.1f);
                            a.setDuration(900);
                            a.setRepeatCount(ObjectAnimator.INFINITE);
                            a.setRepeatMode(ObjectAnimator.REVERSE);
                            a.start();
                            ObjectAnimator b = ObjectAnimator.ofFloat(gv, "scaleY", 0.9f, 1.1f);
                            b.setDuration(900);
                            b.setRepeatCount(ObjectAnimator.INFINITE);
                            b.setRepeatMode(ObjectAnimator.REVERSE);
                            b.start();
                        });
                        break;
                    case EFFECT_NEON_BLINK:
                        gv.setGlow(GlowTextView.GLOW_STRONG);
                        gv.setBlink(true);
                        break;
                    case EFFECT_RAINBOW:
                        gv.setGlow(GlowTextView.GLOW_SOFT);
                        gv.setRainbow(true);
                        break;
                    case EFFECT_BOUNCE:
                        gv.setGlow(GlowTextView.GLOW_SOFT);
                        gv.post(() -> {
                            ObjectAnimator a = ObjectAnimator.ofFloat(gv, "translationY", -80f, 0f);
                            a.setDuration(900);
                            a.setRepeatCount(ObjectAnimator.INFINITE);
                            a.setRepeatMode(ObjectAnimator.RESTART);
                            a.start();
                        });
                        break;
                    case EFFECT_GLOW_STATIC:
                    default:
                        gv.setGlow(GlowTextView.GLOW_SOFT);
                        break;
                }
                mActiveView = gv;
                break;
            }
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mTextContainer.addView(mActiveView, lp);
    }

    private int mapFontStyle(int fontIndex) {
        switch (fontIndex) {
            case FONT_BOLD:         return Typeface.BOLD;
            case FONT_SERIF:        return Typeface.NORMAL; // serif 通过 Typeface 设置
            case FONT_MONOSPACE:    return Typeface.NORMAL;
            case FONT_ITALIC:       return Typeface.ITALIC;
            case FONT_BOLD_ITALIC:  return Typeface.BOLD_ITALIC;
            case FONT_DEFAULT:
            default:                return Typeface.NORMAL;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector != null && mGestureDetector.onTouchEvent(event)
                || super.onTouchEvent(event);
    }

    // ----------- TTS -----------
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int res = mTts.setLanguage(Locale.SIMPLIFIED_CHINESE);
            if (res == TextToSpeech.LANG_MISSING_DATA
                    || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                mTts.setLanguage(Locale.US);
            }
            mTts.setSpeechRate(0.95f);
            mTts.setPitch(1.0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                mUtteranceListener.setHost(this);
                mTts.setOnUtteranceProgressListener(mUtteranceListener);
            }
            mTtsReady = true;
            speakOut();
        } else {
            Toast.makeText(this, "TTS 不可用，请安装语音引擎", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakOut() {
        if (!mTtsReady || mTts == null) return;
        String utteranceId = "mytext_utt";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts.speak(mDisplayText, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            android.os.Bundle params = new android.os.Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            mTts.speak(mDisplayText, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    private void onSpeakDone() {
        if (mTtsLoop && mTtsEnabled && !isFinishing()) {
            mHandler.postDelayed(this::speakOut, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        if (mTts != null) {
            try {
                mTts.stop();
                mTts.shutdown();
            } catch (Exception ignored) {
            }
            mTts = null;
        }
        if (mMusicPlayer != null) {
            mMusicPlayer.stop();
            mMusicPlayer = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    /**
     * TTS 朗读进度监听：完成时若循环则再读一遍。
     */
    private static class UtteranceProgressListenerImpl
            extends android.speech.tts.UtteranceProgressListener {
        private DisplayActivity mHost;

        void setHost(DisplayActivity host) {
            mHost = host;
        }

        @Override
        public void onStart(String utteranceId) { }

        @Override
        public void onDone(String utteranceId) {
            if (mHost != null) mHost.onSpeakDone();
        }

        @Override
        public void onError(String utteranceId) {
            Log.w(TAG, "TTS error: " + utteranceId);
        }
    }
}
