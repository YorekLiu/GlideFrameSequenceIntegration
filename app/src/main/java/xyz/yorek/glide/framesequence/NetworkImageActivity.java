package xyz.yorek.glide.framesequence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.yorek.glide.framesequence.sample.R;

public class NetworkImageActivity extends AppCompatActivity {
    private static final String[] SIMPLE_WEBP = {
            "http://www.gstatic.com/webp/gallery/1.webp",
            "http://www.gstatic.com/webp/gallery/2.webp",
            "http://www.gstatic.com/webp/gallery/3.webp",
            "http://www.gstatic.com/webp/gallery/4.webp",
            "http://www.gstatic.com/webp/gallery/5.webp",
    };
    private static final String[] ALPHA_WEBP = {
            "https://www.gstatic.com/webp/gallery3/1_webp_ll.webp",
            "https://www.gstatic.com/webp/gallery3/2_webp_ll.webp",
            "https://www.gstatic.com/webp/gallery3/3_webp_ll.webp",
            "https://www.gstatic.com/webp/gallery3/4_webp_ll.webp",
            "https://www.gstatic.com/webp/gallery3/5_webp_ll.webp",
            "https://www.gstatic.com/webp/gallery3/1_webp_a.webp",
            "https://www.gstatic.com/webp/gallery3/2_webp_a.webp",
            "https://www.gstatic.com/webp/gallery3/3_webp_a.webp",
            "https://www.gstatic.com/webp/gallery3/4_webp_a.webp",
            "https://www.gstatic.com/webp/gallery3/5_webp_a.webp",
    };
    private static final String[] ANIM_WEBP = {
            "https://raw.githubusercontent.com/1290846731/RecordMySelf/master/chect.webp",
            "https://www.gstatic.com/webp/animated/1.webp",
            "https://qidian.qpic.cn/qidian_common/349573/a36f7d7d8a5e15e1cf3c32d05109467a/0",
            "https://mathiasbynens.be/demo/animated-webp-supported.webp",
            "https://isparta.github.io/compare-webp/image/gif_webp/webp/2.webp", // TODO sample
            "http://osscdn.ixingtu.com/musi_file/20181108/a20540641eb7de9a8bf186261a8ccf57.webp",
            "https://video.billionbottle.com/d6e66dbb883a48f989b1b1d0e035bbbf/image/dynamic/71fcdca947d144b883949bbe368d60c3.gif?x-oss-process=image/resize,w_320/format,webp"
    };
    private static final String[] ANIM_GIF = {
            "https://audiotest.cos.tx.xmcdn.com/storages/bee8-audiotest/84/53/CAoVXoEETGuDAAS2QgAANHsz.gif",
            "https://78.media.tumblr.com/a0c1be3183449f0d207a022c28f4bbf7/tumblr_p1p2cduAiA1wmghc4o1_500.gif",
            "https://78.media.tumblr.com/31ff4ea771940d2403323c1416b81064/tumblr_p1ymv2Xghn1qbt8b8o2_500.gif",
            "https://78.media.tumblr.com/45c7b305f0dbdb9a3c941be1d86aceca/tumblr_p202yd8Jz11uashjdo3_500.gif",
            "https://78.media.tumblr.com/167e9c5a0534d2718853a2e3985d64e2/tumblr_p1yth5CHXk1srs2u0o1_500.gif",
            "https://78.media.tumblr.com/e7548bfe04a9fdadcac440a5802fb570/tumblr_p1zj4dyrxN1u4mwxfo1_500.gif",
    };

    private TextView mTextView;
    private RecyclerView mRecyclerView;
    private ImageAdapter mWebpAdapter;

    private Transformation<Bitmap> mBitmapTrans = null;

    private Spinner mSpinner;
    private Menu mActionMenu;

    private int mImageType = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("FrameSequence加载网络webp、gif");
        setContentView(R.layout.activity_network_image);

        mTextView = (TextView) findViewById(R.id.webp_image_type);
        mRecyclerView = (RecyclerView) findViewById(R.id.webp_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mWebpAdapter = new ImageAdapter(this, getAnimatedWebpUrls());
        mRecyclerView.setAdapter(mWebpAdapter);

        mSpinner = (Spinner) findViewById(R.id.trans_selector);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mBitmapTrans = null;
                        break;
                    case 1:
                        mBitmapTrans = new CenterCrop();
                        break;
                    case 2:
                        mBitmapTrans = new CircleCrop();
                        break;
                    case 3:
                        mBitmapTrans = new RoundedCorners(24);
                        break;
                    case 4:
                        mBitmapTrans = new CenterInside();
                        break;
                    case 5:
                        mBitmapTrans = new FitCenter();
                        break;
                    default:
                        mBitmapTrans = null;
                        break;
                }
                refreshImageData(mImageType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mBitmapTrans = null;
                refreshImageData(mImageType);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_network_image, menu);
        mActionMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        handleMenuItemCheck(item);

        if (id == R.id.static_webp_action) {
            mImageType = 1;
        } else if (id == R.id.alpha_webp_action) {
            mImageType = 2;
        } else if (id == R.id.animate_webp_action) {
            mImageType = 0;
        } else if (id == R.id.animate_gif_action) {
            mImageType = 3;
        }

        refreshImageData(mImageType);
        return true;
    }

    private List<String> getAnimatedWebpUrls() {
        List<String> webpUrls = new ArrayList<>(Arrays.asList(ANIM_WEBP));
        return webpUrls;
    }

    private void refreshImageData(int imageType) {

        mWebpAdapter.setBitmapTransformation(mBitmapTrans);
        switch (imageType) {
            case 0:
                // Animated Webp
                mTextView.setText("animated webp");
                mWebpAdapter.updateData(getAnimatedWebpUrls());
                break;
            case 1:
                // Static lossy webp
                mTextView.setText("static lossy webp");
                mWebpAdapter.updateData(Arrays.asList(SIMPLE_WEBP));
                break;
            case 2:
                // Static lossless webp
                mTextView.setText("static lossless (with alpha) webp");
                mWebpAdapter.updateData(Arrays.asList(ALPHA_WEBP));
                break;
            case 3:
                // Gif
                mTextView.setText("animated gif");
                mWebpAdapter.updateData(Arrays.asList(ANIM_GIF));
                break;
            default:
                break;
        }
    }

    private void handleMenuItemCheck(MenuItem menuItem) {
        if (mActionMenu == null) {
            menuItem.setChecked(true);
            return;
        }

        for (int i = 0; i < mActionMenu.size(); i++) {
            MenuItem item = mActionMenu.getItem(i);
            if (item.getItemId() == menuItem.getItemId()) {
                item.setChecked(true);
            } else {
                item.setChecked(false);
            }
        }
    }
}

class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {
    private static final String TAG = "ImageAdapter";

    private Context mContext;
    private List<String> mImageUrls;

    private Transformation<Bitmap> mBitmapTrans;

    public ImageAdapter(Context context, List<String> urls) {
        mContext = context;
        mImageUrls = new ArrayList<>();
        mImageUrls.addAll(urls);
    }

    public void setBitmapTransformation(Transformation<Bitmap> bitmapTrans) {
        mBitmapTrans = bitmapTrans;
    }

    public void updateData(List<String> urls) {
        mImageUrls.clear();
        mImageUrls.addAll(urls);
        notifyDataSetChanged();
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(mContext).inflate(R.layout.recycler_item_network_image, parent, false);

        return new ImageHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ImageHolder holder, int position) {
        long size = mImageUrls.size();
        if (position < 0 || position >= size) {
            return;
        }

        String url = mImageUrls.get(position);
//        if (holder.imageView instanceof AspectRatioImageView) {
//            AspectRatioImageView view = (AspectRatioImageView)holder.imageView;
//            view.setAspectRatio(720.0f / 1268.0f);
//        }

        loadImage(holder.imageView, url);

        holder.textView.setText(url);
    }

    @Override
    public int getItemCount() {
        return mImageUrls.size();
    }

    private void loadImage(ImageView imageView, String url) {
        GlideRequest<Drawable> glideRequests = GlideApp.with(mContext)
                .load(url)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "resource class: " + resource.getClass().getSimpleName());
                        return false;
                    }
                });
        if (mBitmapTrans != null) {
            glideRequests = glideRequests.transform(mBitmapTrans);
        }
        glideRequests.into(imageView);
    }

    public static class ImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public ImageHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.webp_image);
            textView = (TextView) itemView.findViewById(R.id.webp_text);
        }
    }
}