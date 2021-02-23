package xyz.yorek.glide.framesequence;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

public class ConstraintLayoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cl);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new CLAdapter());
    }

    private static class CLAdapter extends RecyclerView.Adapter<CLAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_cl, parent, false);
            return new Holder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            final int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16F, holder.itemView.getContext().getResources().getDisplayMetrics());
            GlideApp.with(holder.itemView.getContext())
                    .load(R.drawable.aaa)
                    .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(_16dp)))
                    .into(holder.ivImage1);
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        class Holder extends RecyclerView.ViewHolder {
            private final ImageView ivImage1;
            public Holder(@NonNull View itemView) {
                super(itemView);
                ivImage1 = itemView.findViewById(R.id.ivImage1);
            }
        }
    }
}
