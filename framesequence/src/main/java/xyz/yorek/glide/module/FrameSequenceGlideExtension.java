package xyz.yorek.glide.module;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.request.RequestOptions;

import xyz.yorek.glide.framesequence.FrameSequenceDrawable;

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
}