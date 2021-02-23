package xyz.yorek.glide.module;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestOptions;

import xyz.yorek.glide.framesequence.FrameSequenceDrawable;
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
    public static RequestOptions transform(RequestOptions requestBuilder, @NonNull Transformation<Bitmap> transformation) {
        return requestBuilder.transform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
    }

    @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
    public static RequestOptions optionalTransform(RequestOptions requestBuilder, @NonNull Transformation<Bitmap> transformation) {
        return requestBuilder.optionalTransform(FrameSequenceDrawable.class, new FrameSequenceDrawableTransformation(transformation));
    }
}