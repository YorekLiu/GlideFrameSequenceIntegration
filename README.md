# GlideFrameSequenceIntegration 

[![](https://jitpack.io/v/YorekLiu/GlideFrameSequenceIntegration.svg)](https://jitpack.io/#YorekLiu/GlideFrameSequenceIntegration)

可以解码 gif 动图与 webp 动图的，与 glide API 完美结合的库。

优点：

1. 基于 framesequence 的特性，可以完美解码 gif、 webp 动图
2. 自编译 framesequence、giflib 以及 libwebp，实现了采样压缩功能 (downsample)
3. 得益于 Glide 的可扩展性，本库完美支持 Glide，基本 **不需要修改任何业务调用代码** 就可以实现上述功能
4. 完美支持 Glide 的 transform 功能，在使用上就像操作普通 Bitmap 一样，无需额外调用任何其他的方法。

1.3.0版本开始支持Lottie加载，使用请参考sample。

## 1. 使用步骤 Usage

### 1.1 添加依赖

先添加JitPack仓库地址：

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

请先启用 Glide 的 [Generated API](https://bumptech.github.io/glide/doc/generatedapi.html) 相关功能后， 按照 Glide 版本添加对应依赖项即可：

```gradle
// 加载webp、gif
com.github.YorekLiu.GlideFrameSequenceIntegration:framesequence:${lastest-version}'

// 加载lottie
com.github.YorekLiu.GlideFrameSequenceIntegration:lottie:${lastest-version}'
```

> 如何简单判断有没有开启 [Generated API](https://bumptech.github.io/glide/doc/generatedapi.html)？一般来说，能在代码中使用`GlideApp`加载图片即可。

对于部分限制了ndk仅为`armeabi`的工程，为了方便这里也提供了对应的`libframesqeuence.so`以及`libwebp-decoder.so`，实际上自行将`armeabi-v7a`中的复制出来，这是一样的效果。  
文件位于`extra`目录下。

### 1.2 在代码中使用

使用如下方式加载 gif **会**自动替换成 framesequence 解码，此时无需您对业务做任何修改：

```java
Glide.with(xx).load()
GlideApp.with(xx).load()
```

使用如下方式**不会**自动替换，因为您已经显示申明使用了 glide 默认的 gif 解码器：

```java
Glide.with(xx).asGif().load()           // 1
GlideApp.with(xx).asGif().load()        // 2
```

此时，对于1，您可以选择去掉`asGif()`语句。对于2，您也可以选择去掉`asGif()`语句，或者使用 [Generated API](https://bumptech.github.io/glide/doc/generatedapi.html) 将`asGif()`替换成`asFrameSequence()`来显示调用使用 framesequence 解码器。

您可以通过打印`onReousceReady`成功回调中的`resource`的类名判断有没有成功调用 framesequence 进行解码。若打印出的类名为`FrameSequenceDrawable`，则说明集成成功了。下面是示例，更多例子可以查看sample：

```java
GlideApp.with(this)
    .load(...)
    .listener(new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            Log.d("MainActivity", "btnFrameSequence resource class: " + resource.getClass().getSimpleName());
            return false;
        }
    })
    .into(ivGlideDefault);
```

### 1.3 对 transform 的支持

在 Generated API 下，您可以应用任何 transform 在 FrameSequenceDrawable 上，就像 transform 普通的 Bitmap 等一样，无需额外调用任何其他的方法。  
不过值得注意的是，为了更清晰的显示图像，FrameSequenceDrawable 内部会保存一份 transform 后的 Bitmap，这会增加额外的内存消耗。

### 1.4 Proguard Rules

本Library已经内建了，无须开发者额外进行配置。

## 2. 原理 

### 2.1 integration原理

Glide 提供了组件的注册管理器`Registry`，这允许我们对 Glide 里面的内置的一些加载、解码、编码逻辑的组件进行扩展、替换。  

本库将 framesequence 相关的 decoder 插入到了 gif 解码桶的队首，这样就会优先调用 framesequence 的解码器，从而达到了替换 glide 内置 gif 解码器的目的。

```java
@GlideModule
public final class FrameSequenceLibraryModule extends LibraryGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        // insert frame sequence decoder to head to replace glide default gif decoder
        ResourceDecoder<ByteBuffer, FrameSequenceDrawable> byteBufferGifLibDecoder =
                new ByteBufferFrameSequenceDecoder(registry.getImageHeaderParsers(), glide.getBitmapPool());
        registry
                // for animated webp and gif
                .prepend(InputStream.class, FrameSequenceDrawable.class, new StreamFrameSequenceDecoder(registry.getImageHeaderParsers(), byteBufferGifLibDecoder, glide.getArrayPool()))
                .prepend(ByteBuffer.class, FrameSequenceDrawable.class, byteBufferGifLibDecoder);
    }
}
```

为了保证上面这段注册代码能够被 Glide 调用到，请务必保证使用已经启用了 Generated API 的相关功能。

关于这部分，可以查看我之前写过的一篇博文:[Glide6——Glide利用AppGlideModule、LibraryGlideModule更改默认配置、扩展Glide功能；GlideApp与Glide的区别在哪？](https://blog.yorek.xyz/android/3rd-library/glide6/)

### 2.2 framesequence downsample原理

#### 2.3.1 gif

framesequence 底层采用了 giflib 来进行 gif 图片的解析，framesequence 可以理解为 gif 播放的管理者。  

framesequence 会使用双缓冲机制来进行 gif 动画的播放，每一帧实际上都是一个 Bitmap。 framesequence 底层就是将 gif 图片帧上的像素赋值到 bitmap 上的一个过程。而所谓的 downsample，就是在前面赋值的过程中，每隔一定的距离采样一个点，这样就达到了 downsample 的效果。

#### 2.3.2 webp

webp 部分 Java 层代码与 gif 一直。在 native 层，framesequence 解码 webp 依赖于 libwebp，在 decode 每一帧时可以通过设置 option 中的 `use_scaling`、`scaled_width`、`scaled_height` 等参数进行采样解码。

## 3. Thanks

- [giflib](http://giflib.sourceforge.net/gif_lib.html), 本库使用的是5.2.1版本
- [Google framesequence](https://android.googlesource.com/platform/frameworks/ex/+/android-9.0.0_r16/framesequence)，本库使用的是android-9.0.0_r16上的源文件
- [libwebp](https://github.com/webmproject/libwebp)，目前集成版本为1.2.0

## 4. TODO

webp downsample 功能目前在处理含有非关键帧的动图时，会导致非关键帧图片不清晰，部分像素丢失。目前暂时关闭 webp 的 downsample 功能。

