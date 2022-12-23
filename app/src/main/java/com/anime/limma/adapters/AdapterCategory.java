package com.anime.limma.adapters;

import static com.anime.limma.utils.Constant.CATEGORY_LIST;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anime.limma.R;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.models.Category;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AdapterCategory extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Category> categories;

    Context context;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, Category obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterCategory(Context context, List<Category> categories) {
        this.categories = categories;
        this.context = context;
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView category_name;
        public TextView video_count;
        public ImageView category_image;
        public LinearLayout lyt_parent;

        public OriginalViewHolder(View v) {
            super(v);
            category_name = v.findViewById(R.id.category_name);
            video_count = v.findViewById(R.id.video_count);
            category_image = v.findViewById(R.id.category_image);
            lyt_parent = v.findViewById(R.id.lyt_parent);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SharedPref sharedPref = new SharedPref(context);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_list, parent, false);
            return new OriginalViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_grid, parent, false);
            return new OriginalViewHolder(v);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final Category c = categories.get(position);
        final OriginalViewHolder vItem = (OriginalViewHolder) holder;
        SharedPref sharedPref = new SharedPref(context);

        vItem.category_name.setText(c.category_name);

        if (AppConfig.ENABLE_VIDEO_COUNT_ON_CATEGORY) {
            vItem.video_count.setVisibility(View.VISIBLE);
            vItem.video_count.setText(c.video_count + " " + context.getResources().getString(R.string.video_count_text));
        } else {
            vItem.video_count.setVisibility(View.GONE);
        }

        Picasso.get()
                .load(sharedPref.getBaseUrl() + "/upload/category/" + c.category_image)
                .placeholder(R.drawable.ic_thumbnail)
                .into(vItem.category_image);

        vItem.lyt_parent.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, c, position);
            }
        });
    }

    public void setListData(List<Category> items){
        this.categories = items;
        notifyDataSetChanged();
    }

    public void resetListData() {
        this.categories = new ArrayList<>();
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return categories.size();
    }

}