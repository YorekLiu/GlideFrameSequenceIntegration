package xyz.yorek.glide.transformation;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.util.Preconditions;

import java.security.MessageDigest;

import xyz.yorek.glide.framesequence.FrameSequenceDrawable;

public class FrameSequenceDrawableTransformation implements Transformation<FrameSequenceDrawable> {
  private static final String TAG = "FSDTransformation";

  private final Transformation<Bitmap> wrapped;

  public FrameSequenceDrawableTransformation(Transformation<Bitmap> wrapped) {
    this.wrapped = Preconditions.checkNotNull(wrapped);
  }

  @NonNull
  @Override
  public Resource<FrameSequenceDrawable> transform(
          @NonNull final Context context,
          @NonNull Resource<FrameSequenceDrawable> resource,
          final int outWidth,
          final int outHeight) {
      FrameSequenceDrawable drawable = resource.get();
      final BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
      drawable.setOnFrameTransformationListener(new FrameSequenceDrawable.OnFrameTransformationListener() {
          @Override
          public Bitmap transfer(Bitmap bitmap) {
              Resource<Bitmap> bitmapResource = new BitmapResource(bitmap, bitmapPool);
              Resource<Bitmap> transformed = wrapped.transform(context, bitmapResource, outWidth, outHeight);
              return transformed.get();
          }
      });

    return resource;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof FrameSequenceDrawableTransformation) {
      FrameSequenceDrawableTransformation other = (FrameSequenceDrawableTransformation) o;
      return wrapped.equals(other.wrapped);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    wrapped.updateDiskCacheKey(messageDigest);
  }
}
