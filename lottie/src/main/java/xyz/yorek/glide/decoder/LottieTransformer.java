package xyz.yorek.glide.decoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class LottieTransformer implements ResourceTranscoder<LottieComposition, LottieDrawable> {

    @Nullable
    @Override
    public Resource<LottieDrawable> transcode(@NonNull Resource<LottieComposition> toTranscode, @NonNull Options options) {
        LottieComposition composition = toTranscode.get();
        LottieDrawable lottieDrawable = new LottieDrawable();
        lottieDrawable.setComposition(composition);
        lottieDrawable.setRepeatCount(LottieDrawable.INFINITE);
        return new StreamLottieDecoder.LottieDrawableResource(lottieDrawable);
    }
}
