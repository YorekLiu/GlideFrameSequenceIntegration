package xyz.yorek.glide.framesequence;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

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

        final Spinner imageViewSpinner = findViewById(R.id.spinner);
        findViewById(R.id.btnGlideDefault).setOnClickListener(v -> {
            if (ivGlideDefault.getDrawable() == null) {
                GlideApp.with(this)
//                Glide.with(this)
                        .asGif()
                        .load(getDrawable(imageViewSpinner))
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
            } else {
                GlideApp.with(this).clear(ivGlideDefault);
            }
        });
        findViewById(R.id.btnFrameSequence).setOnClickListener(v -> {
            if (ivFrameSequence.getDrawable() == null) {
//                GlideApp.with(this)
                Glide.with(this)
//                    .asFrameSequence()
                        .load(getDrawable(imageViewSpinner))
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
            } else {
                GlideApp.with(this).clear(ivFrameSequence);
            }
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
}