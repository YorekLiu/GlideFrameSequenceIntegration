package xyz.yorek.glide.module;

import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public class LottieGlideExtension {

    private final static RequestOptions DECODE_TYPE_LOTTIE = RequestOptions
            .decodeTypeOf(LottieDrawable.class)
            .lock();

    private LottieGlideExtension() {}

    @GlideType(LottieDrawable.class)
    public static RequestBuilder<LottieDrawable> asLottie(RequestBuilder<LottieDrawable> requestBuilder) {
        return requestBuilder.apply(DECODE_TYPE_LOTTIE);
    }

//    @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
//    public static BaseRequestOptions<?> transform(BaseRequestOptions<?> requestBuilder, @NonNull Transformation<Bitmap> transformation) {
//        return requestBuilder.transform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
//    }
//
//    @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
//    public static BaseRequestOptions<?> optionalTransform(BaseRequestOptions<?> requestBuilder, @NonNull Transformation<Bitmap> transformation) {
//        return requestBuilder.optionalTransform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
//    }

//    @GlideOption
//    public static BaseRequestOptions<?> setLoopBehavior(BaseRequestOptions<?> requestBuilder, int loopBehavior) {
//        return requestBuilder.set(LOOP_BEHAVIOR, loopBehavior);
//    }
//
//    @GlideOption
//    public static BaseRequestOptions<?> setLoopCount(BaseRequestOptions<?> requestBuilder, int loopCount) {
//        return requestBuilder.set(LOOP_COUNT, loopCount);
//    }
}