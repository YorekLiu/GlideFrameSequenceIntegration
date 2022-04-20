package xyz.yorek.glide.framesequence;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import xyz.yorek.glide.framesequence.sample.R;

public class LottieActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Lottie集成");
        setContentView(R.layout.activity_lottie);

        ImageView glideLocal = findViewById(R.id.glideLocal);
        GlideApp.with(this)
                .load("file:///android_asset/lottie/103192-pandarian.json")
                .into(glideLocal);

        ImageView glideRemote = findViewById(R.id.glideRemote);
        GlideApp.with(this)
                .load("https://assets10.lottiefiles.com/packages/lf20_mdyyhg1m.json")
                .into(glideRemote);
    }
}
