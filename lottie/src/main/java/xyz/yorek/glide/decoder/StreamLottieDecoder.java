package xyz.yorek.glide.decoder;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

import java.io.InputStream;

import okio.Okio;

public class StreamLottieDecoder implements ResourceDecoder<InputStream, LottieDrawable> {

    private static final String TAG = StreamLottieDecoder.class.getSimpleName();

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) {
        return true;
    }

    @Override
    public Resource<LottieDrawable> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) {
        LottieResult<LottieComposition> lottieResult = LottieCompositionFactory.fromJsonReaderSync(JsonReader.of(Okio.buffer(Okio.source(source))), null);
        if (lottieResult.getValue() != null) {
            LottieComposition composition = lottieResult.getValue();
            LottieDrawable lottieDrawable = new LottieDrawable();
            lottieDrawable.setComposition(composition);
            lottieDrawable.setRepeatCount(LottieDrawable.INFINITE);
            return new LottieDrawableResource(lottieDrawable);
        } else {
            return null;
        }
    }

    private static class LottieDrawableResource extends DrawableResource<LottieDrawable> {

        private LottieDrawableResource(LottieDrawable drawable) {
            super(drawable);
        }

        @NonNull
        @Override
        public Class<LottieDrawable> getResourceClass() {
            return LottieDrawable.class;
        }

        @Override
        public int getSize() {
            // TODO LottieDrawable memory usage?
            return 0;
        }

        @Override
        public void recycle() {
            drawable.stop();
        }
    }
}