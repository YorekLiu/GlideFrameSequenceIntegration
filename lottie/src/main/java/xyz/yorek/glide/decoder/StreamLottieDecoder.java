package xyz.yorek.glide.decoder;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import okio.Okio;

public class StreamLottieDecoder implements ResourceDecoder<InputStream, LottieComposition> {

    private static final String TAG = StreamLottieDecoder.class.getSimpleName();

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) {
        return true;
    }

    @Override
    public Resource<LottieComposition> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) {
        String cacheKey = cachedKey(source);
        LottieResult<LottieComposition> lottieResult = LottieCompositionFactory.fromJsonReaderSync(JsonReader.of(Okio.buffer(Okio.source(source))), cacheKey);
        if (lottieResult.getValue() != null) {
            return new SimpleResource<>(lottieResult.getValue());
        } else {
            return null;
        }
    }

    private String cachedKey(@NonNull InputStream source) {
        try {
            int length = source.available();
            byte[] bytes = new byte[length];
            source.mark(length);
            source.read(bytes);
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            return Base64.encodeToString(messageDigest.digest(bytes), Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                source.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static class LottieDrawableResource extends DrawableResource<LottieDrawable> {
        LottieDrawableResource(LottieDrawable drawable) {
            super(drawable);
        }

        @NonNull
        @Override
        public Class<LottieDrawable> getResourceClass() {
            return LottieDrawable.class;
        }

        @Override
        public int getSize() {
            // TODO memory size
            return 0;
        }

        @Override
        public void recycle() {
            drawable.stop();
            drawable.cancelAnimation();
        }
    }
}