package xyz.yorek.glide.decoder;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.gif.GifOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import xyz.yorek.glide.AnimatedWebpHeaderParser;
import xyz.yorek.glide.framesequence.FrameSequenceDrawable;

public class StreamFrameSequenceDecoder implements ResourceDecoder<InputStream, FrameSequenceDrawable> {

    private static final String TAG = StreamFrameSequenceDecoder.class.getSimpleName();
    private final ResourceDecoder<ByteBuffer, FrameSequenceDrawable> byteBufferDecoder;
    private final List<ImageHeaderParser> parsers;
    private final ArrayPool byteArrayPool;

    public StreamFrameSequenceDecoder(List<ImageHeaderParser> parsers, ResourceDecoder<ByteBuffer, FrameSequenceDrawable> byteBufferDecoder, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.byteBufferDecoder = byteBufferDecoder;
        this.byteArrayPool = byteArrayPool;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        if (options.get(GifOptions.DISABLE_ANIMATION)) {
            return false;
        }
        ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, source, byteArrayPool);
        if (imageType == ImageHeaderParser.ImageType.GIF) {
            return true;
        } else if (imageType == ImageHeaderParser.ImageType.WEBP_A) {
            AnimatedWebpHeaderParser.WebpImageType webpImageType = AnimatedWebpHeaderParser.getType(source, byteArrayPool);
            return AnimatedWebpHeaderParser.isAnimatedWebpType(webpImageType);
        }

        return false;
    }

    @Override
    public Resource<FrameSequenceDrawable> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {
        byte[] data = inputStreamToBytes(source);
        if (data == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return byteBufferDecoder.decode(byteBuffer, width, height, options);
    }

    private static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16 * 1024;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Error reading data from stream", e);
            }
            return null;
        }
        return buffer.toByteArray();
    }
}