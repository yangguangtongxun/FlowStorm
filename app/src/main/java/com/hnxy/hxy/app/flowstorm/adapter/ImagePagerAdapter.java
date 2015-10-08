package com.hnxy.hxy.app.flowstorm.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.toolbox.NetworkImageView;
import com.hnxy.hxy.app.flowstorm.R;
import com.hnxy.hxy.app.flowstorm.listener.OnItemViewClickListener;
import com.hnxy.hxy.app.flowstorm.utils.Constants;
import com.hnxy.hxy.app.flowstorm.utils.MyApplication;

/**
 * Created by Administrator on 2015/7/15.
 */
public class ImagePagerAdapter extends PagerAdapter {
    Context context;
    String[] imgs;

    OnItemViewClickListener onItemViewClickListener;

    public ImagePagerAdapter(Context context, String[] imgs) {
        this.context = context;
        this.imgs = imgs;
    }

    public void setOnItemViewClickListener(OnItemViewClickListener onItemViewClickListener) {
        this.onItemViewClickListener = onItemViewClickListener;
    }

    @Override
    public int getCount() {
        return imgs.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        NetworkImageView imageView = new NetworkImageView(context);
        imageView.setErrorImageResId(R.mipmap.home_img_default);
        imageView.setDefaultImageResId(R.mipmap.home_img_default);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageUrl(Constants.URL_BASE + imgs[position], MyApplication.getInstance().getImageLoader());
        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onItemViewClickListener != null) {
                    onItemViewClickListener.onItemViewClickListener(-1, position);
                }
            }
        });
        container.addView(imageView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }


}
