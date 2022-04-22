package xyz.yorek.glide.module;

import android.content.Context;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.LibraryGlideModule;

import java.io.InputStream;

import xyz.yorek.glide.decoder.LottieTransformer;
import xyz.yorek.glide.decoder.StreamLottieDecoder;

@GlideModule
public final class LottieLibraryModule extends LibraryGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        registry
                .append(InputStream.class, LottieComposition.class, new StreamLottieDecoder())
                .register(LottieComposition.class, LottieDrawable.class, new LottieTransformer());
    }
}