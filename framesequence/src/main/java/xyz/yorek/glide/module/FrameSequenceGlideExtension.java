package xyz.yorek.glide.module;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.RequestOptions;

import xyz.yorek.glide.framesequence.FrameSequenceDrawable;
import xyz.yorek.glide.framesequence.FrameSequenceOptions;
import xyz.yorek.glide.transformation.FrameSequenceDrawableTransformation;

@GlideExtension
public class FrameSequenceGlideExtension {

    private final static RequestOptions DECODE_TYPE_FRAME_SEQUENCE = RequestOptions
            .decodeTypeOf(FrameSequenceDrawable.class)
            .lock();

    private FrameSequenceGlideExtension() {}

    @GlideType(FrameSequenceDrawable.class)
    public static RequestBuilder<FrameSequenceDrawable> asFrameSequence(RequestBuilder<FrameSequenceDrawable> requestBuilder) {
        return requestBuilder.apply(DECODE_TYPE_FRAME_SEQUENCE);
    }

    @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
    public static BaseRequestOptions<?> transform(BaseRequestOptions<?> requestBuilder, @NonNull Transformation<Bitmap> transformation) {
        return requestBuilder.transform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
    }

    @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
    public static BaseRequestOptions<?> optionalTransform(BaseRequestOptions<?> requestBuilder, @NonNull Transformation<Bitmap> transformation) {
        return requestBuilder.optionalTransform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
    }

    @GlideOption
    public static BaseRequestOptions<?> setLoopBehavior(BaseRequestOptions<?> requestBuilder, int loopBehavior) {
        return requestBuilder.set(FrameSequenceOptions.LOOP_BEHAVIOR, loopBehavior);
    }

    @GlideOption
    public static BaseRequestOptions<?> setLoopCount(BaseRequestOptions<?> requestBuilder, int loopCount) {
        return requestBuilder.set(FrameSequenceOptions.LOOP_COUNT, loopCount);
    }

    @GlideOption
    public static BaseRequestOptions<?> disableFrameSequenceSample(BaseRequestOptions<?> requestBuilder) {
        return requestBuilder.set(FrameSequenceOptions.ENABLE_SAMPLE, false);
    }
}