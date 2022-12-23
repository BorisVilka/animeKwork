package com.anime.limma.fragments;

import static com.anime.limma.utils.Constant.VIDEO_LIST_COMPACT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.anime.limma.R;
import com.anime.limma.activities.ActivityVideoDetail;
import com.anime.limma.activities.ActivityVideoDetailOffline;
import com.anime.limma.activities.MainActivity;
import com.anime.limma.adapters.AdapterVideo;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.databases.sqlite.DbFavorite;
import com.anime.limma.models.Video;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FragmentFavorite extends Fragment {

    List<Video> videos = new ArrayList<>();
    View rootView;
    AdapterVideo adapterVideo;
    DbFavorite dbFavorite;
    RecyclerView recyclerView;
    LinearLayout lytNoFavorite;
    private BottomSheetDialog mBottomSheetDialog;
    SharedPref sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_favorite, container, false);

        if (getActivity() != null)
            sharedPref = new SharedPref(getActivity());

        lytNoFavorite = rootView.findViewById(R.id.lyt_no_favorite);
        recyclerView = rootView.findViewById(R.id.recyclerView);
        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.spacing_small), 0, 0);
        }

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        dbFavorite = new DbFavorite(getActivity());
        adapterVideo = new AdapterVideo(getActivity(), recyclerView, videos);
        recyclerView.setAdapter(adapterVideo);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayData(dbFavorite.getAllData());
    }

    public void displayData(List<Video> posts) {
        List<Video> favorites = new ArrayList<>();
        if (posts != null && posts.size() > 0) {
            favorites.addAll(posts);
        }
        adapterVideo.resetListData();
        adapterVideo.insertDataWithNativeAd(favorites);
        showNoItemView(favorites.size() == 0);

        adapterVideo.setOnItemClickListener((v, obj, position) -> {
            if (Tools.isConnect(getActivity())) {
                Intent intent = new Intent(getActivity(), ActivityVideoDetail.class);
                intent.putExtra(Constant.EXTRA_OBJC, obj);
                startActivity(intent);

                if (getActivity() != null)
                    ((MainActivity) getActivity()).showInterstitialAd();
            } else {
                Intent intent = new Intent(getActivity(), ActivityVideoDetailOffline.class);
                intent.putExtra(Constant.POSITION, position);
                intent.putExtra(Constant.KEY_VIDEO_CATEGORY_ID, obj.cat_id);
                intent.putExtra(Constant.KEY_VIDEO_CATEGORY_NAME, obj.category_name);
                intent.putExtra(Constant.KEY_VID, obj.vid);
                intent.putExtra(Constant.KEY_VIDEO_TITLE, obj.video_title);
                intent.putExtra(Constant.KEY_VIDEO_URL, obj.video_url);
                intent.putExtra(Constant.KEY_VIDEO_ID, obj.video_id);
                intent.putExtra(Constant.KEY_VIDEO_THUMBNAIL, obj.video_thumbnail);
                intent.putExtra(Constant.KEY_VIDEO_DURATION, obj.video_duration);
                intent.putExtra(Constant.KEY_VIDEO_DESCRIPTION, obj.video_description);
                intent.putExtra(Constant.KEY_VIDEO_TYPE, obj.video_type);
                intent.putExtra(Constant.KEY_TOTAL_VIEWS, obj.total_views);
                intent.putExtra(Constant.KEY_DATE_TIME, obj.date_time);
                startActivity(intent);
            }
        });

        adapterVideo.setOnItemOverflowClickListener((view, obj, position) -> {
            if (getActivity() != null)
                showBottomSheetDialog(getActivity().findViewById(R.id.coordinatorLayout), obj);
        });

    }

    public void showBottomSheetDialog(View parentView, Video video) {
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.include_bottom_sheet, null);

        FrameLayout lytBottomSheet = view.findViewById(R.id.bottom_sheet);

        TextView txtFavorite = view.findViewById(R.id.txt_favorite);

        ImageView imgFavorite = view.findViewById(R.id.img_favorite);
        ImageView imgShare = view.findViewById(R.id.img_share);

        if (sharedPref.getIsDarkTheme()) {
            lytBottomSheet.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getActivity()), R.drawable.bg_rounded_dark));
            imgFavorite.setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorWhite));
            imgShare.setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorWhite));
        } else {
            lytBottomSheet.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getActivity()), R.drawable.bg_rounded_default));
            imgFavorite.setColorFilter(ContextCompat.getColor(getActivity(), R.color.grey_dark));
            imgShare.setColorFilter(ContextCompat.getColor(getActivity(), R.color.grey_dark));
        }

        LinearLayout btnFavorite = view.findViewById(R.id.btn_favorite);
        LinearLayout btnShare = view.findViewById(R.id.btn_share);

        btnFavorite.setOnClickListener(v -> {
            List<Video> videos = dbFavorite.getFavRow(video.vid);
            if (videos.size() == 0) {
                dbFavorite.addToFavorite(new Video(
                        video.category_name,
                        video.vid,
                        video.video_title,
                        video.video_url,
                        video.video_id,
                        video.video_thumbnail,
                        video.video_duration,
                        video.video_description,
                        video.video_type,
                        video.total_views,
                        video.date_time
                ));
                Snackbar.make(parentView, getString(R.string.msg_favorite_added), Snackbar.LENGTH_SHORT).show();
                imgFavorite.setImageResource(R.drawable.ic_fav);

            } else {
                if (videos.get(0).vid.equals(video.vid)) {
                    dbFavorite.RemoveFav(new Video(video.vid));
                    Snackbar.make(parentView, getString(R.string.msg_favorite_removed), Snackbar.LENGTH_SHORT).show();
                    imgFavorite.setImageResource(R.drawable.ic_fav_outline);
                    refreshData();
                }
            }
            mBottomSheetDialog.dismiss();
        });

        btnShare.setOnClickListener(v -> {
            Tools.shareContent(getActivity(), video.video_title, getResources().getString(R.string.share_text));
            mBottomSheetDialog.dismiss();
        });

        if (sharedPref.getIsDarkTheme()) {
            this.mBottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.SheetDialogDark);
        } else {
            this.mBottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.SheetDialogLight);
        }
        this.mBottomSheetDialog.setContentView(view);

        mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(dialog -> mBottomSheetDialog = null);

        dbFavorite = new DbFavorite(getActivity());
        List<Video> videos = dbFavorite.getFavRow(video.vid);
        if (videos.size() == 0) {
            txtFavorite.setText(getString(R.string.favorite_add));
            imgFavorite.setImageResource(R.drawable.ic_fav_outline);
        } else {
            if (videos.get(0).vid.equals(video.vid)) {
                txtFavorite.setText(getString(R.string.favorite_remove));
                imgFavorite.setImageResource(R.drawable.ic_fav);
            }
        }

    }

    private void showNoItemView(boolean show) {
        if (show) {
            lytNoFavorite.setVisibility(View.VISIBLE);
        } else {
            lytNoFavorite.setVisibility(View.GONE);
        }
    }

    public void refreshData() {
        adapterVideo.resetListData();
        displayData(dbFavorite.getAllData());
//        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
//        fragmentTransaction.detach(this).attach(this).commit();
    }

}
