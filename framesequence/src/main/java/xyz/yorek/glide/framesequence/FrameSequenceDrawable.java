package xyz.yorek.glide.framesequence;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public class FrameSequenceDrawable extends Drawable implements Animatable, Runnable {
    private static final String TAG = "FrameSequence";
    /**
     * These constants are chosen to imitate common browser behavior for WebP/GIF.
     * If other decoders are added, this behavior should be moved into the WebP/GIF decoders.
     *
     * Note that 0 delay is undefined behavior in the GIF standard.
     */
    private static final long MIN_DELAY_MS = 20;
    private static final long DEFAULT_DELAY_MS = 100;
    private static final Object sLock = new Object();
    private static HandlerThread sDecodingThread;
    private static Handler sDecodingThreadHandler;
    private static void initializeDecodingThread() {
        synchronized (sLock) {
            if (sDecodingThread != null) return;
            sDecodingThread = new HandlerThread("FrameSequence decoding thread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sDecodingThread.start();
            sDecodingThreadHandler = new Handler(sDecodingThread.getLooper());
        }
    }
    public interface OnFinishedListener {
        /**
         * Called when a FrameSequenceDrawable has finished looping.
         *
         * Note that this is will not be called if the drawable is explicitly
         * stopped, or marked invisible.
         */
        void onFinished(FrameSequenceDrawable drawable);
    }
    // update by yorek.liu >> begin
    public interface OnFrameTransformationListener {
        Bitmap transfer(Bitmap bitmap);
    }
    // update by yorek.liu >> end
    public interface BitmapProvider {
        /**
         * Called by FrameSequenceDrawable to acquire an 8888 Bitmap with minimum dimensions.
         */
        Bitmap acquireBitmap(int minWidth, int minHeight);
        /**
         * Called by FrameSequenceDrawable to release a Bitmap it no longer needs. The Bitmap
         * will no longer be used at all by the drawable, so it is safe to reuse elsewhere.
         *
         * This method may be called by FrameSequenceDrawable on any thread.
         */
        void releaseBitmap(Bitmap bitmap);
    }
    private static final BitmapProvider sAllocatingBitmapProvider = new BitmapProvider() {
        @Override
        public Bitmap acquireBitmap(int minWidth, int minHeight) {
            return Bitmap.createBitmap(minWidth, minHeight, Bitmap.Config.ARGB_8888);
        }
        @Override
        public void releaseBitmap(Bitmap bitmap) {}
    };
    /**
     * Register a callback to be invoked when a FrameSequenceDrawable finishes looping.
     *
     * @see #setLoopBehavior(int)
     */
    public void setOnFinishedListener(OnFinishedListener onFinishedListener) {
        mOnFinishedListener = onFinishedListener;
    }
    // update by yorek.liu >> begin
    public void setOnFrameTransformationListener(OnFrameTransformationListener onFrameTransformationListener) {
        mOnFrameTransformationListener = onFrameTransformationListener;
    }
    // update by yorek.liu >> end
    /**
     * Loop a finite number of times, which can be set using setLoopCount. Default to loop once.
     */
    public static final int LOOP_FINITE = 1;
    /**
     * Loop continuously. The OnFinishedListener will never be called.
     */
    public static final int LOOP_INF = 2;
    /**
     * Use loop count stored in source data, or LOOP_ONCE if not present.
     */
    public static final int LOOP_DEFAULT = 3;
    /**
     * Loop only once.
     *
     * @deprecated Use LOOP_FINITE instead.
     */
    @Deprecated
    public static final int LOOP_ONCE = LOOP_FINITE;
    /**
     * Define looping behavior of frame sequence.
     *
     * Must be one of LOOP_ONCE, LOOP_INF, LOOP_DEFAULT, or LOOP_FINITE.
     */
    public void setLoopBehavior(int loopBehavior) {
        mLoopBehavior = loopBehavior;
    }
    /**
     * Set the number of loops in LOOP_FINITE mode. The number must be a positive integer.
     */
    public void setLoopCount(int loopCount) {
        mLoopCount = loopCount;
    }
    private final FrameSequence mFrameSequence;
    private final FrameSequence.State mFrameSequenceState;
    private final Paint mPaint;
    private BitmapShader mFrontBitmapShader;
    private BitmapShader mBackBitmapShader;
    private final Rect mSrcRect;
    private boolean mCircleMaskEnabled;
    //Protects the fields below
    private final Object mLock = new Object();
    private final BitmapProvider mBitmapProvider;
    private boolean mDestroyed = false;
    private Bitmap mFrontBitmap;
    private Bitmap mBackBitmap;
    private static final int STATE_SCHEDULED = 1;
    private static final int STATE_DECODING = 2;
    private static final int STATE_WAITING_TO_SWAP = 3;
    private static final int STATE_READY_TO_SWAP = 4;
    private int mState;
    private int mCurrentLoop;
    private int mLoopBehavior = LOOP_DEFAULT;
    private int mLoopCount = 1;
    // update by yorek.liu >> begin
    private final int mSampleSize;
    // update by yorek.liu >> end
    private long mLastSwap;
    private long mNextSwap;
    private int mNextFrameToDecode;
    private OnFinishedListener mOnFinishedListener;
    // update by yorek.liu >> begin
    private OnFrameTransformationListener mOnFrameTransformationListener;
    // save the transferred bitmap to draw, don't scale the bitmap to make it clear.
    private Bitmap mBackTransferredBitmap;
    // update by yorek.liu >> end
    private final RectF mTempRectF = new RectF();
    /**
     * Runs on decoding thread, only modifies mBackBitmap's pixels
     */
    private final Runnable mDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            int nextFrame;
            Bitmap bitmap;
            synchronized (mLock) {
                if (mDestroyed) return;
                nextFrame = mNextFrameToDecode;
                if (nextFrame < 0) {
                    return;
                }
                bitmap = mBackBitmap;
                mState = STATE_DECODING;
            }
            int lastFrame = nextFrame - 2;
            boolean exceptionDuringDecode = false;
            long invalidateTimeMs = 0;
            try {
                invalidateTimeMs = mFrameSequenceState.getFrame(nextFrame, bitmap, lastFrame, mSampleSize);
                // update by yorek.liu >> begin
                if (mOnFrameTransformationListener != null) {
                    mBackTransferredBitmap = mOnFrameTransformationListener.transfer(bitmap);
                    setBounds(0, 0, mBackTransferredBitmap.getWidth(), mBackTransferredBitmap.getHeight());
                }
                // update by yorek.liu >> end
            } catch(Exception e) {
                // Exception during decode: continue, but delay next frame indefinitely.
                Log.e(TAG, "exception during decode: " + e);
                exceptionDuringDecode = true;
            }
            if (invalidateTimeMs < MIN_DELAY_MS) {
                invalidateTimeMs = DEFAULT_DELAY_MS;
            }
            boolean schedule = false;
            Bitmap bitmapToRelease = null;
            // update by yorek.liu >> begin
            Bitmap transferredBitmapToRelease = null;
            // update by yorek.liu >> end
            synchronized (mLock) {
                if (mDestroyed) {
                    bitmapToRelease = mBackBitmap;
                    // update by yorek.liu >> begin
                    transferredBitmapToRelease = mBackTransferredBitmap;
                    mBackTransferredBitmap = null;
                    // update by yorek.liu >> end
                    mBackBitmap = null;
                } else if (mNextFrameToDecode >= 0 && mState == STATE_DECODING) {
                    schedule = true;
                    mNextSwap = exceptionDuringDecode ? Long.MAX_VALUE : invalidateTimeMs + mLastSwap;
                    mState = STATE_WAITING_TO_SWAP;
                }
            }
            if (schedule) {
                scheduleSelf(FrameSequenceDrawable.this, mNextSwap);
            }
            if (bitmapToRelease != null) {
                // destroy the bitmap here, since there's no safe way to get back to
                // drawable thread - drawable is likely detached, so schedule is noop.
                mBitmapProvider.releaseBitmap(bitmapToRelease);
            }
            // update by yorek.liu >> begin
            if (transferredBitmapToRelease != null) {
                mBitmapProvider.releaseBitmap(transferredBitmapToRelease);
            }
            // update by yorek.liu >> end
        }
    };
    private final Runnable mFinishedCallbackRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                mNextFrameToDecode = -1;
                mState = 0;
            }
            if (mOnFinishedListener != null) {
                mOnFinishedListener.onFinished(FrameSequenceDrawable.this);
            }
        }
    };
    private static Bitmap acquireAndValidateBitmap(BitmapProvider bitmapProvider,
                                                   int minWidth, int minHeight) {
        Bitmap bitmap = bitmapProvider.acquireBitmap(minWidth, minHeight);
        if (bitmap.getWidth() < minWidth
                || bitmap.getHeight() < minHeight
                || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new IllegalArgumentException("Invalid bitmap provided");
        }
        return bitmap;
    }
    public FrameSequenceDrawable(FrameSequence frameSequence) {
        this(frameSequence, sAllocatingBitmapProvider);
    }
    // update by yorek.liu >> begin
    public FrameSequenceDrawable(FrameSequence frameSequence, BitmapProvider bitmapProvider) {
        this(frameSequence, sAllocatingBitmapProvider, 1);
    }
    public FrameSequenceDrawable(FrameSequence frameSequence, BitmapProvider bitmapProvider, int sampleSize) {
        if (frameSequence == null || bitmapProvider == null) throw new IllegalArgumentException();
        mSampleSize = sampleSize;
        mFrameSequence = frameSequence;
        mFrameSequenceState = frameSequence.createState();
        final int width = frameSequence.getWidth();
        final int height = frameSequence.getHeight();
        final int downsampledWidth = width / sampleSize;
        final int downsampledHeight = height / sampleSize;
        mBitmapProvider = bitmapProvider;
        mFrontBitmap = acquireAndValidateBitmap(bitmapProvider, downsampledWidth, downsampledHeight);
        mBackBitmap = acquireAndValidateBitmap(bitmapProvider, downsampledWidth, downsampledHeight);
        mSrcRect = new Rect(0, 0, downsampledWidth, downsampledHeight);
        mPaint = new Paint();
        mPaint.setFilterBitmap(true);
        mFrontBitmapShader
                = new BitmapShader(mFrontBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mBackBitmapShader
                = new BitmapShader(mBackBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mLastSwap = 0;
        mNextFrameToDecode = -1;
        mFrameSequenceState.getFrame(0, mFrontBitmap, -1, sampleSize);
        initializeDecodingThread();
    }
    // update by yorek.liu >> end
    /**
     * Pass true to mask the shape of the animated drawing content to a circle.
     *
     * <p> The masking circle will be the largest circle contained in the Drawable's bounds.
     * Masking is done with BitmapShader, incurring minimal additional draw cost.
     */
    public final void setCircleMaskEnabled(boolean circleMaskEnabled) {
        if (mCircleMaskEnabled != circleMaskEnabled) {
            mCircleMaskEnabled = circleMaskEnabled;
            // Anti alias only necessary when using circular mask
            mPaint.setAntiAlias(circleMaskEnabled);
            invalidateSelf();
        }
    }
    public final boolean getCircleMaskEnabled() {
        return mCircleMaskEnabled;
    }
    private void checkDestroyedLocked() {
        if (mDestroyed) {
            throw new IllegalStateException("Cannot perform operation on recycled drawable");
        }
    }
    public boolean isDestroyed() {
        synchronized (mLock) {
            return mDestroyed;
        }
    }
    /**
     * Marks the drawable as permanently recycled (and thus unusable), and releases any owned
     * Bitmaps drawable to its BitmapProvider, if attached.
     *
     * If no BitmapProvider is attached to the drawable, recycle() is called on the Bitmaps.
     */
    public void destroy() {
        if (mBitmapProvider == null) {
            throw new IllegalStateException("BitmapProvider must be non-null");
        }
        Bitmap bitmapToReleaseA;
        Bitmap bitmapToReleaseB = null;
        // update by yorek.liu >> begin
        Bitmap transferredBitmapToReleaseB = null;
        // update by yorek.liu >> end
        synchronized (mLock) {
            checkDestroyedLocked();
            bitmapToReleaseA = mFrontBitmap;
            mFrontBitmap = null;
            if (mState != STATE_DECODING) {
                bitmapToReleaseB = mBackBitmap;
                mBackBitmap = null;
                // update by yorek.liu >> begin
                transferredBitmapToReleaseB = mBackTransferredBitmap;
                mBackTransferredBitmap = null;
                // update by yorek.liu >> end
            }
            mDestroyed = true;
        }
        // For simplicity and safety, we don't destroy the state object here
        mBitmapProvider.releaseBitmap(bitmapToReleaseA);
        if (bitmapToReleaseB != null) {
            mBitmapProvider.releaseBitmap(bitmapToReleaseB);
        }
        // update by yorek.liu >> begin
        if (transferredBitmapToReleaseB != null) {
            mBitmapProvider.releaseBitmap(transferredBitmapToReleaseB);
        }
        // update by yorek.liu >> end
    }
    @Override
    protected void finalize() throws Throwable {
        try {
            mFrameSequenceState.destroy();
        } finally {
            super.finalize();
        }
    }
    @Override
    public void draw(Canvas canvas) {
        synchronized (mLock) {
            checkDestroyedLocked();
            if (mState == STATE_WAITING_TO_SWAP) {
                // may have failed to schedule mark ready runnable,
                // so go ahead and swap if swapping is due
                if (mNextSwap - SystemClock.uptimeMillis() <= 0) {
                    mState = STATE_READY_TO_SWAP;
                }
            }
            if (isRunning() && mState == STATE_READY_TO_SWAP) {
                // Because draw has occurred, the view system is guaranteed to no longer hold a
                // reference to the old mFrontBitmap, so we now use it to produce the next frame
                Bitmap tmp = mBackBitmap;
                mBackBitmap = mFrontBitmap;
                mFrontBitmap = tmp;
                BitmapShader tmpShader = mBackBitmapShader;
                mBackBitmapShader = mFrontBitmapShader;
                mFrontBitmapShader = tmpShader;
                mLastSwap = SystemClock.uptimeMillis();
                boolean continueLooping = true;
                if (mNextFrameToDecode == mFrameSequence.getFrameCount() - 1) {
                    mCurrentLoop++;
                    if ((mLoopBehavior == LOOP_FINITE && mCurrentLoop == mLoopCount) ||
                            (mLoopBehavior == LOOP_DEFAULT && mCurrentLoop == mFrameSequence.getDefaultLoopCount())) {
                        continueLooping = false;
                    }
                }
                if (continueLooping) {
                    scheduleDecodeLocked();
                } else {
                    scheduleSelf(mFinishedCallbackRunnable, 0);
                }
            }
        }
        if (mCircleMaskEnabled) {
            final Rect bounds = getBounds();
            final int bitmapWidth = getIntrinsicWidth();
            final int bitmapHeight = getIntrinsicHeight();
            final float scaleX = 1.0f * bounds.width() / bitmapWidth;
            final float scaleY = 1.0f * bounds.height() / bitmapHeight;
            canvas.save();
            // scale and translate to account for bounds, so we can operate in intrinsic
            // width/height (so it's valid to use an unscaled bitmap shader)
            canvas.translate(bounds.left, bounds.top);
            canvas.scale(scaleX, scaleY);
            final float unscaledCircleDiameter = Math.min(bounds.width(), bounds.height());
            final float scaledDiameterX = unscaledCircleDiameter / scaleX;
            final float scaledDiameterY = unscaledCircleDiameter / scaleY;
            // Want to draw a circle, but we have to compensate for canvas scale
            mTempRectF.set(
                    (bitmapWidth - scaledDiameterX) / 2.0f,
                    (bitmapHeight - scaledDiameterY) / 2.0f,
                    (bitmapWidth + scaledDiameterX) / 2.0f,
                    (bitmapHeight + scaledDiameterY) / 2.0f);
            mPaint.setShader(mFrontBitmapShader);
            canvas.drawOval(mTempRectF, mPaint);
            canvas.restore();
        } else {
            mPaint.setShader(null);
            // update by yorek.liu >> begin
//            canvas.drawBitmap(mFrontBitmap, mSrcRect, getBounds(), mPaint);
            if (mBackTransferredBitmap != null) {
                canvas.drawBitmap(mBackTransferredBitmap, null, getBounds(), mPaint);
            } else {
                canvas.drawBitmap(mFrontBitmap, mSrcRect, getBounds(), mPaint);
            }
            // update by yorek.liu >> end
        }
    }
    private void scheduleDecodeLocked() {
        mState = STATE_SCHEDULED;
        mNextFrameToDecode = (mNextFrameToDecode + 1) % mFrameSequence.getFrameCount();
        sDecodingThreadHandler.post(mDecodeRunnable);
    }
    @Override
    public void run() {
        // set ready to swap as necessary
        boolean invalidate = false;
        synchronized (mLock) {
            if (mNextFrameToDecode >= 0 && mState == STATE_WAITING_TO_SWAP) {
                mState = STATE_READY_TO_SWAP;
                invalidate = true;
            }
        }
        if (invalidate) {
            invalidateSelf();
        }
    }
    @Override
    public void start() {
        if (!isRunning()) {
            synchronized (mLock) {
                checkDestroyedLocked();
                if (mState == STATE_SCHEDULED) return; // already scheduled
                mCurrentLoop = 0;
                scheduleDecodeLocked();
            }
        }
    }
    @Override
    public void stop() {
        if (isRunning()) {
            unscheduleSelf(this);
        }
    }
    @Override
    public boolean isRunning() {
        synchronized (mLock) {
            return mNextFrameToDecode > -1 && !mDestroyed;
        }
    }
    @Override
    public void unscheduleSelf(Runnable what) {
        synchronized (mLock) {
            mNextFrameToDecode = -1;
            mState = 0;
        }
        super.unscheduleSelf(what);
    }
    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (!visible) {
            stop();
        } else if (restart || changed) {
            stop();
            start();
        }
        return changed;
    }
    // drawing properties
    @Override
    public void setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
    }
    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }
    // update by yorek.liu >> begin
    @Override
    public int getIntrinsicWidth() {
        if (mBackTransferredBitmap != null) {
            return mBackTransferredBitmap.getWidth();
        } else {
            return mSampleSize == 0 ? mFrameSequence.getWidth() : mFrameSequence.getWidth() / mSampleSize;
        }
    }
    @Override
    public int getIntrinsicHeight() {
        if (mBackTransferredBitmap != null) {
            return mBackTransferredBitmap.getHeight();
        } else {
            return mSampleSize == 0 ? mFrameSequence.getHeight() : mFrameSequence.getHeight() / mSampleSize;
        }
    }
    // update by yorek.liu >> end
    @Override
    public int getOpacity() {
        return mFrameSequence.isOpaque() ? PixelFormat.OPAQUE : PixelFormat.TRANSPARENT;
    }

    // update by yorek.liu >> begin
    public int getSize() {
        return getBitmapByteSize(mFrontBitmap) + getBitmapByteSize(mBackBitmap) + getBitmapByteSize(mBackTransferredBitmap);
    }

    private static int getBitmapByteSize(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        return getBitmapByteSize(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    }

    private static int getBitmapByteSize(int width, int height, @Nullable Bitmap.Config config) {
        return width * height * getBytesPerPixel(config);
    }

    private static int getBytesPerPixel(@Nullable Bitmap.Config config) {
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }

        int bytesPerPixel;
        switch (config) {
            case ALPHA_8:
                bytesPerPixel = 1;
                break;
            case RGB_565:
            case ARGB_4444:
                bytesPerPixel = 2;
                break;
            case RGBA_F16:
                bytesPerPixel = 8;
                break;
            case ARGB_8888:
            default:
                bytesPerPixel = 4;
                break;
        }
        return bytesPerPixel;
    }
    // update by yorek.liu >> end
}