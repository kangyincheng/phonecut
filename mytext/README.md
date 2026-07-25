# mytext · 演唱会大字

一个用于演唱会、Live 现场等场景的安卓 App，可以在手机屏幕上打出炫酷大字体，
让远处也能看清，同时支持同步朗读（TTS）和背景音乐。

## 功能特性

- **大字显示**：自适应屏幕，远距离可读
- **多种字体风格**：默认 / 粗体 / 衬线 / 等宽 / 斜体 / 粗斜体
- **多种特效**：
  - 静态发光
  - 呼吸脉冲（缩放）
  - 霓虹闪烁
  - 彩虹渐变
  - 向左 / 向右滚动
  - 向上 / 向下滚动
  - 跳动出现
- **9 种文字颜色** + 9 种深色背景
- **同步朗读（TTS）**：可选循环朗读
- **背景音乐**：内置 3 段实时合成的节奏（无版权问题）
- **全屏沉浸式**：自动隐藏状态栏 / 导航栏，保持屏幕常亮

## 项目结构

```
mytext/
├── settings.gradle
├── build.gradle                       # 根项目
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle                   # 模块配置
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/mytext/
        │   ├── MainActivity.java        # 输入界面
        │   ├── DisplayActivity.java     # 全屏展示
        │   ├── GlowTextView.java        # 发光 / 彩虹 / 闪烁自定义 View
        │   ├── MarqueeTextView.java     # 上下左右滚动自定义 View
        │   └── ToneMusicPlayer.java     # 实时合成背景音乐
        └── res/
            ├── layout/                  # activity_main / activity_display / item_color
            ├── values/                  # strings / colors / themes / arrays / integers
            ├── drawable/                # 背景、按钮、应用图标
            └── anim/                    # pulse / blink / scroll_* / bounce（备用）
```

## 编译与运行

### 使用 Android Studio（推荐）

1. Android Studio → `Open` → 选择 `mytext/` 目录
2. 等待 Gradle Sync 完成
3. 连接安卓手机（开启 USB 调试）或启动模拟器
4. 点击 `Run ▶`

### 使用命令行

```bash
cd mytext
# 首次需要生成 gradle wrapper（如未提供 gradlew）
gradle wrapper --gradle-version 8.2

# Debug 构建
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug
```

> 最低支持 Android 5.0（API 21），目标 SDK 34。
> Gradle 8.2、AGP 8.1.0。

## 使用说明

1. 打开 App，在输入框输入要显示的文字（例如：`我们爱你！`）
2. 选择字体大小、风格、特效、文字颜色、背景颜色
3. 选择是否开启 TTS 朗读 / 循环朗读
4. 选择背景音乐节奏（或「无音乐」）
5. 点击「开始展示」进入全屏模式
6. 在屏幕上**单击**即可退出

## 实现要点

### 1. 大字渲染：`GlowTextView`
- 直接在 `Canvas` 上绘制文字，避免 `TextView` 在巨大字号下的渲染开销
- `setShadowLayer` 实现霓虹光晕（柔和 / 强烈两档）
- `LinearGradient` 着色器实现彩虹渐变，配合 `ValueAnimator` 平移色相
- `setAlpha` 动画实现霓虹闪烁
- **二分搜索**自动计算最大可用字号，保证文字完整可见

### 2. 滚动效果：`MarqueeTextView`
- 用 `ValueAnimator` 驱动 `offset`，根据方向计算 x/y 坐标
- 4 个方向：左、右、上、下
- 文字长度自适应滚动时长

### 3. TTS 同步朗读：`TextToSpeech`
- 默认中文（无中文引擎时回退英文）
- `UtteranceProgressListener` 监听朗读完成，循环模式下自动再次朗读

### 4. 程序化合成音乐：`ToneMusicPlayer`
- 用 `AudioTrack` 实时合成 PCM 数据，**不依赖任何音频文件**
- ADSR 包络 + 三倍泛音 + 底鼓噪声，三种节奏循环
- 完全自研，无版权风险

## 自定义与扩展

### 添加自定义字体
将 `.ttf` 文件放入 `app/src/main/res/font/`，然后在 `GlowTextView` / `MarqueeTextView` 中：
```java
Typeface tf = ResourcesCompat.getFont(context, R.font.my_font);
mPaint.setTypeface(tf);
```

### 使用本地音乐文件
将 `.mp3` 放入 `app/src/main/res/raw/`，用 `MediaPlayer` 替换 `ToneMusicPlayer`：
```java
MediaPlayer mp = MediaPlayer.create(context, R.raw.my_song);
mp.setLooping(true);
mp.start();
```

## 已知限制

- 应用图标为简单矢量图，可按需替换为 PNG 资源
- 程序化合成音乐较为简单，建议替换为真实音频以获得更好效果
- TTS 效果依赖系统语音引擎，不同设备表现略有差异

## License

MIT
