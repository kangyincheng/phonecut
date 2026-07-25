package com.example.mytext;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 程序化合成背景音乐播放器。
 *
 * 不依赖任何音频文件，使用 AudioTrack 实时合成 PCM。
 * 提供 3 段不同节奏 / 旋律的循环音乐，避免版权问题。
 *
 * 使用：
 *   player = new ToneMusicPlayer();
 *   player.start(0);   // 选择节奏 0/1/2
 *   player.stop();
 */
public class ToneMusicPlayer {

    private static final int SAMPLE_RATE = 44100;
    private static final double TWO_PI = 2 * Math.PI;

    /** 三段节奏 / 旋律定义（基频 + 节拍） */
    private static final double[][][] PATTERNS = {
            // 节奏一：欢快电子（C - G - Am - F）
            {
                    {261.63, 0.5}, {392.00, 0.5},
                    {220.00, 0.5}, {349.23, 0.5},
            },
            // 节奏二：抒情（C - Em - F - G）
            {
                    {261.63, 0.8}, {329.63, 0.8},
                    {349.23, 0.8}, {392.00, 0.8},
            },
            // 节奏三：舞曲（A - A - E - D）
            {
                    {440.00, 0.25}, {440.00, 0.25},
                    {329.63, 0.25}, {293.66, 0.25},
            },
    };

    private AudioTrack mTrack;
    private Thread mThread;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private int mPatternIndex = 0;
    private volatile float mVolume = 0.5f;

    public void start(int patternIndex) {
        if (patternIndex < 0 || patternIndex >= PATTERNS.length) {
            patternIndex = 0;
        }
        mPatternIndex = patternIndex;
        stop();

        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize <= 0) bufferSize = SAMPLE_RATE / 2;

        mTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        mTrack.play();
        mRunning.set(true);
        mThread = new Thread(this::runLoop, "ToneMusicPlayer");
        mThread.setPriority(Thread.MAX_PRIORITY);
        mThread.start();
    }

    public void stop() {
        mRunning.set(false);
        if (mThread != null) {
            try {
                mThread.join(300);
            } catch (InterruptedException ignored) {
            }
            mThread = null;
        }
        if (mTrack != null) {
            try {
                mTrack.stop();
                mTrack.release();
            } catch (IllegalStateException ignored) {
            }
            mTrack = null;
        }
    }

    public void setVolume(float v) {
        mVolume = Math.max(0f, Math.min(1f, v));
        if (mTrack != null) {
            mTrack.setVolume(mVolume);
        }
    }

    public boolean isPlaying() {
        return mRunning.get();
    }

    private void runLoop() {
        double[][] pattern = PATTERNS[mPatternIndex];
        // 一个 ADSR 包络 + 鼓点
        double[] drums = {80.0, 0};   // 底鼓：低频短促
        while (mRunning.get()) {
            for (double[] note : pattern) {
                if (!mRunning.get()) return;
                playNote(note[0], note[1]);
                // 节奏一、三带底鼓
                if (mPatternIndex == 0 || mPatternIndex == 2) {
                    playDrum((int) (drums[0]), 0.1);
                }
            }
        }
    }

    private void playNote(double freq, double seconds) {
        int samples = (int) (seconds * SAMPLE_RATE);
        short[] buffer = new short[samples];
        // 旋律主频 + 三倍泛音，加一点 vibrato
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = adsrEnvelope(i, samples);
            double vibrato = 1.0 + 0.005 * Math.sin(TWO_PI * 5.0 * t);
            double wave = Math.sin(TWO_PI * freq * vibrato * t)
                    + 0.4 * Math.sin(TWO_PI * 2 * freq * vibrato * t)
                    + 0.15 * Math.sin(TWO_PI * 3 * freq * vibrato * t);
            double v = wave * env * 0.45 * mVolume * 2;
            // clamp
            if (v > 1.0) v = 1.0;
            if (v < -1.0) v = -1.0;
            buffer[i] = (short) (v * Short.MAX_VALUE);
        }
        writeBuffer(buffer);
    }

    private void playDrum(int freqHz, double seconds) {
        int samples = (int) (seconds * SAMPLE_RATE);
        short[] buffer = new short[samples];
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = Math.exp(-t * 18.0); // 短促衰减
            double wave = Math.sin(TWO_PI * freqHz * t) * env;
            // 加点白噪声让鼓更"实"
            wave += (Math.random() * 2 - 1) * env * 0.3;
            double v = wave * 0.6 * mVolume * 2;
            if (v > 1.0) v = 1.0;
            if (v < -1.0) v = -1.0;
            buffer[i] = (short) (v * Short.MAX_VALUE);
        }
        writeBuffer(buffer);
    }

    /** 简易 ADSR */
    private double adsrEnvelope(int i, int total) {
        double a = 0.05;   // attack 5%
        double d = 0.20;   // decay 20%
        double s = 0.6;    // sustain level
        double r = 0.25;    // release 25%
        double pos = i / (double) total;
        if (pos < a) return pos / a;
        if (pos < a + d) return 1.0 - (1.0 - s) * ((pos - a) / d);
        if (pos < 1.0 - r) return s;
        return s * (1.0 - (pos - (1.0 - r)) / r);
    }

    private void writeBuffer(short[] buffer) {
        if (mTrack == null || !mRunning.get()) return;
        mTrack.write(buffer, 0, buffer.length);
    }
}
