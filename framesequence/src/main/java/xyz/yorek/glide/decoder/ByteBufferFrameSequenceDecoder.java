package xyz.yorek.glide.decoder;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.load.resource.gif.GifOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import xyz.yorek.glide.AnimatedWebpHeaderParser;
import xyz.yorek.glide.framesequence.FrameSequence;
import xyz.yorek.glide.framesequence.FrameSequenceDrawable;
import xyz.yorek.glide.framesequence.FrameSequenceOptions;

public class ByteBufferFrameSequenceDecoder implements ResourceDecoder<ByteBuffer, FrameSequenceDrawable> {

    private static boolean DEBUG = false;

    private static final String TAG = ByteBufferFrameSequenceDecoder.class.getSimpleName();
    private final List<ImageHeaderParser> parsers;
    private final FrameSequenceDrawable.BitmapProvider mProvider;

    public ByteBufferFrameSequenceDecoder(List<ImageHeaderParser> parsers, final BitmapPool bitmapPool) {
        this.parsers = parsers;
        this.mProvider = new FrameSequenceDrawable.BitmapProvider() {
            @Override
            public Bitmap acquireBitmap(int minWidth, int minHeight) {
                return bitmapPool.getDirty(minWidth, minHeight, Bitmap.Config.ARGB_8888);
            }

            @Override
            public void releaseBitmap(Bitmap bitmap) {
                bitmapPool.put(bitmap);
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        if (options.get(GifOptions.DISABLE_ANIMATION)) {
            return false;
        }
        ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, source);
        if (imageType == ImageHeaderParser.ImageType.GIF) {
            return true;
        } else if (imageType == ImageHeaderParser.ImageType.WEBP_A) {
            source.rewind();
            AnimatedWebpHeaderParser.WebpImageType webpImageType = AnimatedWebpHeaderParser.getType(source);
            return AnimatedWebpHeaderParser.isAnimatedWebpType(webpImageType);
        }

        return false;
    }

    @Override
    public FrameSequenceDrawableResource decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {
        // FIXME delete me after supporting webp downsample
        boolean isAnimatedWebp = AnimatedWebpHeaderParser.isAnimatedWebpType(AnimatedWebpHeaderParser.getType(source));
        source.rewind();
//        isAnimatedWebp = false;

        FrameSequence frameSequence = FrameSequence.decodeByteBuffer(source);
        if (frameSequence == null) {
            return null;
        }
        FrameSequenceDrawable drawable;
        if (options.get(FrameSequenceOptions.ENABLE_SAMPLE) && !isAnimatedWebp) {
            int sampleSize = calcSampleSize(frameSequence.getWidth(), frameSequence.getHeight(), width, height);
            drawable = new FrameSequenceDrawable(frameSequence, mProvider, sampleSize);
        } else {
            drawable = new FrameSequenceDrawable(frameSequence, mProvider);
        }
        Integer loopBehavior = options.get(FrameSequenceOptions.LOOP_BEHAVIOR);
        if (loopBehavior != null) {
            drawable.setLoopBehavior(loopBehavior);
        }
        Integer loopCount = options.get(FrameSequenceOptions.LOOP_COUNT);
        if (loopCount != null) {
            drawable.setLoopCount(loopCount);
        }
        return new FrameSequenceDrawableResource(drawable);
    }

    private int calcSampleSize(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int exactSampleSize = Math.min(sourceHeight / targetHeight,
                sourceWidth / targetWidth);
        int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
        // than 0.
        int sampleSize = Math.max(1, powerOfTwoSampleSize);
        if (DEBUG && sampleSize > 1) {
            Log.v(TAG, "Downsampling GIF"
                    + ", sampleSize: " + sampleSize
                    + ", target dimens: [" + targetWidth + "x" + targetHeight + "]"
                    + ", actual dimens: [" + sourceWidth + "x" + sourceHeight + "]");
        }
        return sampleSize;
    }


    private static class FrameSequenceDrawableResource extends DrawableResource<FrameSequenceDrawable> {

        private FrameSequenceDrawableResource(FrameSequenceDrawable drawable) {
            super(drawable);
        }

        @NonNull
        @Override
        public Class<FrameSequenceDrawable> getResourceClass() {
            return FrameSequenceDrawable.class;
        }

        @Override
        public int getSize() {
            return drawable.getSize();
        }

        @Override
        public void recycle() {
            drawable.stop();
            drawable.destroy();
        }
    }
}