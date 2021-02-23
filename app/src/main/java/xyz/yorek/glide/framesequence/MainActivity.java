package xyz.yorek.glide.framesequence;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView ivGlideDefault = findViewById(R.id.ivGlideDefault);
        final ImageView ivFrameSequence = findViewById(R.id.ivFrameSequence);

        final TextView etSize = findViewById(R.id.etSize);
        findViewById(R.id.btnSubmitSize).setOnClickListener(v -> {
            double size = Double.parseDouble(etSize.getText().toString());
            updateImageViewSize(size, ivGlideDefault, ivFrameSequence);
        });

        final int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16F, getResources().getDisplayMetrics());
        final Spinner imageViewSpinner = findViewById(R.id.spinner);
        findViewById(R.id.btnGlideDefault).setOnClickListener(v -> {
            GlideApp.with(this)
                    .asGif()
                    .load(getDrawable(imageViewSpinner))
                    .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(_16dp)))
                    .listener(new RequestListener<GifDrawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("MainActivity", "btnGlideDefault resource class: " + resource.getClass().getSimpleName());
                            return false;
                        }
                    })
                    .into(ivGlideDefault);
        });
        findViewById(R.id.btnFrameSequence).setOnClickListener(v -> {
            GlideApp.with(this)
//                    .asFrameSequence()
                    .load(getDrawable(imageViewSpinner))
                    .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(_16dp)))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("MainActivity", "btnFrameSequence resource class: " + resource.getClass().getSimpleName());
                            return false;
                        }
                    })
                    .into(ivFrameSequence);
        });
    }

    private void updateImageViewSize(double size, ImageView... imageViews) {
        for (ImageView imageView: imageViews) {
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            layoutParams.width = (int) size;
            layoutParams.height = (int) size;
            imageView.setLayoutParams(layoutParams);
        }
    }

    private int getDrawable(Spinner spinner) {
        return spinner.getSelectedItemPosition() == 1 ? R.drawable.aaa : R.drawable.bbb;
    }

//    public void jumpToAnotherActivity(View view) {
//        startActivity(new Intent(this, ConstraintLayoutActivity.class));
//    }
}