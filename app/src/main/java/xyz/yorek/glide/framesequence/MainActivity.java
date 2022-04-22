package xyz.yorek.glide.framesequence;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;

import xyz.yorek.glide.framesequence.sample.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("FrameSequence效果");
        setContentView(R.layout.activity_main);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MainAdapter(this));
    }
}

class MainAdapter extends RecyclerView.Adapter<MainAdapter.Holder> {

    private final Context context;
    private final List<ImageModel> modelList = new ArrayList<>();

    {
        modelList.add(new ImageModel(R.drawable.gif_336x336, "gif图可完美展示，也完美支持transform", 336, 336));
        modelList.add(new ImageModel(R.drawable.webp_144x144, "webp动图Glide默认不支持，使用FS后可以支持", 144, 144));
        modelList.add(new ImageModel(R.drawable.gif_144x144, "上面webp动图的gif版本，都可以支持", 144, 144));
        modelList.add(new ImageModel(R.drawable.webp_990x1050, "webp动图Glide默认不支持，使用FS后可以支持", 990, 1050));
    }

    MainAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.recycler_item_main, parent, false));
    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImageModel imageModel = modelList.get(position);
        String text = context.getResources().getResourceEntryName(imageModel.resId) + "\n" + imageModel.desc;
        holder.tvImageUrl.setText(text);

        loadImage(imageModel, 1.0f, holder.ivGlideBuiltin, holder.ivFs);
        loadImage(imageModel, 0.5f, holder.ivGlideBuiltinSmall, holder.ivFsSmall);
//        loadImage(imageModel, 2.0f, holder.ivGlideBuiltinBig, holder.ivFsBig);
    }

    private void loadImage(ImageModel imageModel, float scale, ImageView glideBuiltin, ImageView fs) {
        int width = (int) (imageModel.width * scale);
        int height = (int) (imageModel.height * scale);

        updateViewSize(glideBuiltin, width, height);
        updateViewSize(fs, width, height);

        GlideApp.with(context)
                .asGif()
                .load(imageModel.resId)
                .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(32)))
                .into(glideBuiltin);

        GlideApp.with(context)
                .asFrameSequence()
                // 控制播放次数
//                .setLoopBehavior(FrameSequenceDrawable.LOOP_FINITE)
//                .setLoopCount(2)
                .load(imageModel.resId)
                .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(32)))
                .into(fs);
    }

    private void updateViewSize(View view, int width, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvImageUrl;

        ImageView ivGlideBuiltin;
        ImageView ivFs;

        ImageView ivGlideBuiltinSmall;
        ImageView ivFsSmall;

        ImageView ivGlideBuiltinBig;
        ImageView ivFsBig;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvImageUrl = itemView.findViewById(R.id.tvImageUrl);
            ivGlideBuiltin = itemView.findViewById(R.id.ivGlideBuiltin);
            ivFs = itemView.findViewById(R.id.ivFs);
            ivGlideBuiltinSmall = itemView.findViewById(R.id.ivGlideBuiltinSmall);
            ivFsSmall = itemView.findViewById(R.id.ivFsSmall);
            ivGlideBuiltinBig = itemView.findViewById(R.id.ivGlideBuiltinBig);
            ivFsBig = itemView.findViewById(R.id.ivFsBig);
        }
    }
}

class ImageModel {
    public int resId;
    public String desc;
    public int width;
    public int height;

    public ImageModel(int resId, String desc, int width, int height) {
        this.resId = resId;
        this.desc = desc;
        this.width = width;
        this.height = height;
    }
}