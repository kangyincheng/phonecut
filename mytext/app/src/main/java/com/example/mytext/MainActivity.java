package com.example.mytext;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText mEtInput;
    private Spinner mSpFont;
    private Spinner mSpEffect;
    private Spinner mSpMusic;
    private SeekBar mSbTextSize;
    private TextView mTvTextSizeHint;
    private SwitchMaterial mSwTts;
    private SwitchMaterial mSwLoop;

    private int mSelectedColorIndex = 0;
    private int mSelectedBgColorIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEtInput = findViewById(R.id.etInput);
        mSpFont = findViewById(R.id.spFont);
        mSpEffect = findViewById(R.id.spEffect);
        mSpMusic = findViewById(R.id.spMusic);
        mSbTextSize = findViewById(R.id.sbTextSize);
        mTvTextSizeHint = findViewById(R.id.tvTextSizeHint);
        mSwTts = findViewById(R.id.swTts);
        mSwLoop = findViewById(R.id.swLoop);

        // 适配 Spinner
        ArrayAdapter<CharSequence> fontAdapter = ArrayAdapter.createFromResource(
                this, R.array.font_styles, android.R.layout.simple_spinner_item);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpFont.setAdapter(fontAdapter);

        ArrayAdapter<CharSequence> effectAdapter = ArrayAdapter.createFromResource(
                this, R.array.effect_styles, android.R.layout.simple_spinner_item);
        effectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpEffect.setAdapter(effectAdapter);

        ArrayAdapter<CharSequence> musicAdapter = ArrayAdapter.createFromResource(
                this, R.array.music_names, android.R.layout.simple_spinner_item);
        musicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpMusic.setAdapter(musicAdapter);

        // 字体大小
        updateTextSizeHint(mSbTextSize.getProgress());
        mSbTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextSizeHint(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 颜色列表
        RecyclerView rvColors = findViewById(R.id.rvColors);
        RecyclerView rvBgColors = findViewById(R.id.rvBgColors);

        int[] textColors = loadColors(R.array.text_colors);
        int[] bgColors = loadColors(R.array.bg_colors);

        ColorAdapter textAdapter = new ColorAdapter(textColors, index -> {
            mSelectedColorIndex = index;
        });
        textAdapter.setSelected(0);
        rvColors.setAdapter(textAdapter);
        rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        ColorAdapter bgAdapter = new ColorAdapter(bgColors, index -> {
            mSelectedBgColorIndex = index;
        });
        bgAdapter.setSelected(0);
        rvBgColors.setAdapter(bgAdapter);
        rvBgColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> startDisplay());

        // 默认填充示例文本
        mEtInput.setText("我们爱你！");
        mEtInput.setSelection(mEtInput.getText().length());
    }

    private void updateTextSizeHint(int progress) {
        // 30 ~ 330 sp 范围
        int sp = 30 + progress;
        mTvTextSizeHint.setText(sp + "sp");
    }

    private int[] loadColors(int arrayRes) {
        TypedArray ta = getResources().obtainTypedArray(arrayRes);
        int[] colors = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            colors[i] = ta.getColor(i, Color.WHITE);
        }
        ta.recycle();
        return colors;
    }

    private void startDisplay() {
        String text = mEtInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "请先输入文字", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DisplayActivity.class);
        intent.putExtra(DisplayActivity.EXTRA_TEXT, text);
        intent.putExtra(DisplayActivity.EXTRA_FONT_INDEX, mSpFont.getSelectedItemPosition());
        intent.putExtra(DisplayActivity.EXTRA_EFFECT_INDEX, mSpEffect.getSelectedItemPosition());
        intent.putExtra(DisplayActivity.EXTRA_COLOR_INDEX, mSelectedColorIndex);
        intent.putExtra(DisplayActivity.EXTRA_BG_COLOR_INDEX, mSelectedBgColorIndex);
        intent.putExtra(DisplayActivity.EXTRA_TEXT_SIZE, 30 + mSbTextSize.getProgress());
        intent.putExtra(DisplayActivity.EXTRA_TTS_ENABLED, mSwTts.isChecked());
        intent.putExtra(DisplayActivity.EXTRA_TTS_LOOP, mSwLoop.isChecked());
        intent.putExtra(DisplayActivity.EXTRA_MUSIC_INDEX, mSpMusic.getSelectedItemPosition());
        startActivity(intent);
    }

    // --------- 颜色适配器 ---------
    static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.VH> {
        private final int[] mColors;
        private final OnSelectedListener mListener;
        private int mSelected = 0;

        interface OnSelectedListener {
            void onSelected(int index);
        }

        ColorAdapter(int[] colors, OnSelectedListener listener) {
            mColors = colors;
            mListener = listener;
        }

        void setSelected(int index) {
            mSelected = index;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            int color = mColors[position];
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke(3, position == mSelected ? Color.WHITE : 0x33FFFFFF);
            holder.color.setBackground(drawable);
            holder.itemView.setOnClickListener(v -> {
                int old = mSelected;
                mSelected = holder.getAdapterPosition();
                notifyItemChanged(old);
                notifyItemChanged(mSelected);
                if (mListener != null) mListener.onSelected(mSelected);
            });
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }

        static class VH extends RecyclerView.ViewHolder {
            View color;

            VH(@NonNull View itemView) {
                super(itemView);
                color = itemView.findViewById(R.id.vColor);
            }
        }
    }
}
