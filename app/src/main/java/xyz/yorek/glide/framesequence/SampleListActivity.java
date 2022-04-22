package xyz.yorek.glide.framesequence;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.yorek.glide.framesequence.sample.R;

public class SampleListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Glide扩展示例列表");
        setContentView(R.layout.activity_sample_list);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new SampleListAdapter(this, clazz -> startActivity(new Intent(this, clazz))));
    }
}

class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.Holder> {

    private final Context context;
    private final List<SampleModel> modelList = new ArrayList<>();
    private OnJumpActivityCallback onJumpActivityCallback;

    private final View.OnClickListener onClickListener = v -> {
        if (v.getTag() instanceof Class<?>) {
            onJumpActivityCallback.onJumpActivity((Class<?>) v.getTag());
        }
    };

    {
        modelList.add(new SampleModel("Glide默认解码器与FrameSequence效果对比", MainActivity.class));
        modelList.add(new SampleModel("FrameSequence加载网络webp、gif", NetworkImageActivity.class));
        modelList.add(new SampleModel("Lottie集成", LottieActivity.class));
    }

    SampleListAdapter(Context context, OnJumpActivityCallback onJumpActivityCallback) {
        this.context = context;
        this.onJumpActivityCallback = onJumpActivityCallback;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SampleModel imageModel = modelList.get(position);
        holder.textView.setText(imageModel.title);

        holder.itemView.setTag(imageModel.clazz);
        holder.itemView.setOnClickListener(onClickListener);
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView textView;

        public Holder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    interface OnJumpActivityCallback {
        void onJumpActivity(Class<?> clazz);
    }
}

class SampleModel {
    public String title;
    public Class<?> clazz;

    public SampleModel(String title, Class<?> clazz) {
        this.title = title;
        this.clazz = clazz;
    }
}