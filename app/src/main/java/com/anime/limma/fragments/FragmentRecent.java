package com.anime.limma.fragments;

import static com.anime.limma.utils.Constant.VIDEO_LIST_COMPACT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anime.limma.R;
import com.anime.limma.activities.ActivityVideoDetail;
import com.anime.limma.activities.MainActivity;
import com.anime.limma.adapters.AdapterVideo;
import com.anime.limma.callbacks.CallbackListVideo;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.models.Video;
import com.anime.limma.rests.ApiInterface;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.EqualSpacingItemDecoration;
import com.anime.limma.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentRecent extends Fragment {

    View rootView;
    private RecyclerView recyclerView;
    private AdapterVideo adapterVideo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackListVideo> callbackCall = null;
    private ShimmerFrameLayout shimmerFrameLayout;
    private int postTotal = 0;
    private int failedPage = 0;
    SharedPref sharedPref;
    Tools tools;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recent, container, false);
        if (getActivity() != null) {
            tools = new Tools(getActivity());
            sharedPref = new SharedPref(getActivity());
        }
        sharedPref.setDefaultSortHome();

        setHasOptionsMenu(true);

        shimmerFrameLayout = rootView.findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout_home);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = rootView.findViewById(R.id.recyclerView);

        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.spacing_small), 0, getResources().getDimensionPixelOffset(R.dimen.spacing_small));
        }

        recyclerView.addItemDecoration(new EqualSpacingItemDecoration(0));
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterVideo = new AdapterVideo(getActivity(), recyclerView, new ArrayList<Video>());
        recyclerView.setAdapter(adapterVideo);

        // on item list clicked
        adapterVideo.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityVideoDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            ((MainActivity) getActivity()).showInterstitialAd();
        });

        adapterVideo.setOnItemOverflowClickListener((view, obj, position) -> tools.showBottomSheetDialog(Objects.requireNonNull(getActivity()), getActivity().findViewById(R.id.coordinatorLayout), obj));

        // detect when scroll reach bottom
        adapterVideo.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterVideo.resetListData();
            requestAction(1);
        });

        requestAction(1);
        initShimmerLayout();
        onSortButtonClickListener();

        return rootView;
    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterVideo.getItemCount() - current_page);
        if (postTotal > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterVideo.setLoaded();
        }
    }

    private void displayApiResult(final List<Video> videos) {
        adapterVideo.insertDataWithNativeAd(videos);
        swipeProgress(false);
        if (videos.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());

        if (sharedPref.getCurrentSortHome() == 0) {
            callbackCall = apiInterface.getVideos(page_no, AppConfig.LOAD_MORE, Constant.MOST_POPULAR, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentSortHome() == 1) {
            callbackCall = apiInterface.getVideos(page_no, AppConfig.LOAD_MORE, Constant.ADDED_OLDEST, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentSortHome() == 2) {
            callbackCall = apiInterface.getVideos(page_no, AppConfig.LOAD_MORE, Constant.ADDED_NEWEST, AppConfig.REST_API_KEY);
        } else {
            callbackCall = apiInterface.getVideos(page_no, AppConfig.LOAD_MORE, Constant.ADDED_NEWEST, AppConfig.REST_API_KEY);
        }

        callbackCall.enqueue(new Callback<CallbackListVideo>() {
            @Override
            public void onResponse(Call<CallbackListVideo> call, Response<CallbackListVideo> response) {
                CallbackListVideo resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.posts);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackListVideo> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterVideo.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterVideo.setLoading();
        }
        new Handler().postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        shimmerFrameLayout.stopShimmer();
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = rootView.findViewById(R.id.lyt_failed_home);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item_home);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
        });
    }

    private void initShimmerLayout() {
        View lyt_shimmer_default = rootView.findViewById(R.id.lyt_shimmer_default);
        View lyt_shimmer_compact = rootView.findViewById(R.id.lyt_shimmer_compact);
        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            lyt_shimmer_default.setVisibility(View.GONE);
            lyt_shimmer_compact.setVisibility(View.VISIBLE);
        } else {
            lyt_shimmer_default.setVisibility(View.VISIBLE);
            lyt_shimmer_compact.setVisibility(View.GONE);
        }
    }

    private void onSortButtonClickListener() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.btnSort.setOnClickListener(view -> {
                String[] items = getResources().getStringArray(R.array.dialog_single_choice_array);
                int itemSelected = sharedPref.getCurrentSortHome();
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.title_sort)
                        .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                            if (callbackCall != null && callbackCall.isExecuted())
                                callbackCall.cancel();
                            adapterVideo.resetListData();
                            requestAction(1);
                            sharedPref.updateSortHome(position);
                            dialogInterface.dismiss();
                        })
                        .show();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
