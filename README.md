# GlideFrameSequenceIntegration 

[ ![Download](https://api.bintray.com/packages/yorekliu/maven/GlideFrameSequenceIntegration/images/download.svg) ](https://bintray.com/yorekliu/maven/GlideFrameSequenceIntegration/_latestVersion)

结合 giflib 和带降采特性的 framesequence 的 native gif 解码器，用以取代 glide 默认的 gif 解码器。

## 使用步骤 Usage

### 添加依赖

请先启用 Glide 的 [Generated API](https://bumptech.github.io/glide/doc/generatedapi.html) 相关功能后， 按照 Glide 版本添加对应依赖项即可：

```gradle
// glide 版本在4.9.0及以后
implementation 'xyz.yorek.glide:framesequence-integration:${lastest-version}'

// glide 版本在4.8.0及以前
implementation 'xyz.yorek.glide:framesequence-integration-480:${lastest-version}'
```

> 如何简单判断有没有开启 [Generated API](https://bumptech.github.io/glide/doc/generatedapi.html)？一般来说，能在代码中使用`GlideApp`加载图片即可。

若无法下载到本库，请添加如下仓库地址：

```gradle
maven { url "https://dl.bintray.com/yorekliu/maven" }
```

对于部分限制了ndk仅为`armeabi`的工程，为了方便这里也提供了对应的so，实际上自行将`armeabi-v7a`中的复制出来，这是一样的效果。  
文件位于`extra`目录下。

### 在代码中使用

集成`framesequence`完毕后，使用如下方式加载 gif **会**自动替换成 framesequence 解码，此时无需您对业务做任何修改：

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

### 对 transform 的支持

在 Generated API 下，您可以应用任何 transform 在 FrameSequenceDrawable 上，就像 transform 普通的Bitmap等一样，无需额外调用任何其他的方法。  
不过值得注意的是，为了更清晰的显示图像，FrameSequenceDrawable 内部会保存一份 transform 后的 Bitmap，这会增加额外的内存消耗。

### Proguard Rules

本Library已经配置好了，无须开发者额外进行配置。

## 原理 

### integration原理

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
        registry.prepend(Registry.BUCKET_GIF, InputStream.class, FrameSequenceDrawable.class, new StreamFrameSequenceDecoder(registry.getImageHeaderParsers(), byteBufferGifLibDecoder, glide.getArrayPool()))
                .prepend(Registry.BUCKET_GIF, ByteBuffer.class, FrameSequenceDrawable.class, byteBufferGifLibDecoder);
    }
}
```

为了保证上面这段注册代码能够被 Glide 调用到，请务必保证使用已经启用了 Generated API 的相关功能。

关于这部分，可以查看我之前写过的一篇博文:[Glide6——Glide利用AppGlideModule、LibraryGlideModule更改默认配置、扩展Glide功能；GlideApp与Glide的区别在哪？](https://blog.yorek.xyz/android/3rd-library/glide6/)

### framesequence downsample原理

framesequence 底层采用了 giflib 来进行 gif 图片的解析，framesequence 可以理解为 gif 播放的管理者。  

framesequence 会使用双缓冲机制来进行 gif 动画的播放，每一帧实际上都是一个 Bitmap。 framesequence 底层就是将gif 图片帧上的像素赋值到 bitmap 上的一个过程。 而所谓的 downsample，就是在前面赋值的过程中，每隔一定的距离采样一个点，这样就达到了 downsample 的效果。

## Thanks

- [giflib](http://giflib.sourceforge.net/gif_lib.html), 本库使用的是5.2.1版本
- [Google framesequence](https://android.googlesource.com/platform/frameworks/ex/+/android-9.0.0_r16/framesequence)，本库使用的是android-9.0.0_r16上的源文件

## TODO

Google android-9.0.0_r16 上的 framesequence 还有webp动图的部分，不过也依赖于`libwebp-decode`的这个库，而且也需要实现 downsample 操作，后面有空了考虑整合进去。

