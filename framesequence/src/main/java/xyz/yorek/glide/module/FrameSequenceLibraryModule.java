package xyz.yorek.glide.module;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.module.LibraryGlideModule;

import java.io.InputStream;
import java.nio.ByteBuffer;

import xyz.yorek.glide.decoder.ByteBufferFrameSequenceDecoder;
import xyz.yorek.glide.decoder.StreamFrameSequenceDecoder;
import xyz.yorek.glide.framesequence.FrameSequenceDrawable;

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