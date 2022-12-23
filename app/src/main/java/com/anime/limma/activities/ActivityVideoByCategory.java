package com.anime.limma.activities;

import static com.anime.limma.utils.Constant.VIDEO_LIST_COMPACT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anime.limma.R;
import com.anime.limma.adapters.AdapterVideo;
import com.anime.limma.callbacks.CallbackCategoryDetails;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.models.Category;
import com.anime.limma.models.Video;
import com.anime.limma.rests.ApiInterface;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.AdsManager;
import com.anime.limma.utils.AppBarLayoutBehavior;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityVideoByCategory extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterVideo adapterVideo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackCategoryDetails> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    private Category category;
    SharedPref sharedPref;
    CoordinatorLayout parentView;
    private ShimmerFrameLayout shimmerFrameLayout;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_category_details);
        adsManager = new AdsManager(this);
        Tools.setNavigation(this);

        tools = new Tools(this);
        sharedPref = new SharedPref(this);
        sharedPref.setDefaultSortVideos();

        //adsPref = new AdsPref(this);
       // adsManager = new AdsManager(this);
        //adsManager.loadBannerAd(BANNER_CATEGORY_DETAIL);
        //adsManager.loadInterstitialAd(INTERSTITIAL_POST_LIST, adsPref.getInterstitialAdInterval());

        AppBarLayout appBarLayout = findViewById(R.id.appbarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        category = (Category) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        parentView = findViewById(R.id.coordinatorLayout);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.spacing_small), 0, 0);
        }

        //set data and list adapter
        adapterVideo = new AdapterVideo(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterVideo);

        // on item list clicked
        adapterVideo.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityVideoDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            InterstitialAd interstitialAd = new InterstitialAd(this);
            interstitialAd.setAdUnitId("R-M-1774888-3");
            interstitialAd.setInterstitialAdEventListener(new InterstitialAdEventListener() {
                @Override
                public void onAdLoaded() {
                    interstitialAd.show();
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {

                }

                @Override
                public void onAdShown() {

                }

                @Override
                public void onAdDismissed() {

                }

                @Override
                public void onAdClicked() {

                }

                @Override
                public void onLeftApplication() {

                }

                @Override
                public void onReturnedToApplication() {

                }

                @Override
                public void onImpression(@Nullable ImpressionData impressionData) {

                }
            });
            interstitialAd.loadAd(new AdRequest.Builder().build());
        });

        adapterVideo.setOnItemOverflowClickListener((view, obj, position) -> tools.showBottomSheetDialog(ActivityVideoByCategory.this, parentView, obj));

        // detect when scroll reach bottom
        adapterVideo.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) {
                callbackCall.cancel();
            }
            adapterVideo.resetListData();
            requestAction(1);
        });

        requestAction(1);
        initShimmerLayout();
        setupToolbar();

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

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(category.category_name);
        }
    }

    private void displayApiResult(final List<Video> videos) {
        adapterVideo.insertDataWithNativeAd(videos);
        swipeProgress(false);
        if (videos.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());

        if (sharedPref.getCurrentSortVideos() == 0) {
            callbackCall = apiInterface.getCategoryVideos(category.cid, page_no, AppConfig.LOAD_MORE, Constant.MOST_POPULAR, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentSortVideos() == 1) {
            callbackCall = apiInterface.getCategoryVideos(category.cid, page_no, AppConfig.LOAD_MORE, Constant.ADDED_OLDEST, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentSortVideos() == 2) {
            callbackCall = apiInterface.getCategoryVideos(category.cid, page_no, AppConfig.LOAD_MORE, Constant.ADDED_NEWEST, AppConfig.REST_API_KEY);
        } else {
            callbackCall = apiInterface.getCategoryVideos(category.cid, page_no, AppConfig.LOAD_MORE, Constant.ADDED_NEWEST, AppConfig.REST_API_KEY);
        }

        callbackCall.enqueue(new Callback<CallbackCategoryDetails>() {
            @Override
            public void onResponse(Call<CallbackCategoryDetails> call, Response<CallbackCategoryDetails> response) {
                CallbackCategoryDetails resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.posts);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackCategoryDetails> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterVideo.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getApplicationContext())) {
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
        new Handler().postDelayed(() -> requestPostApi(page_no), Constant.DELAY_TIME);
    }

    private void showFailedView(boolean show, String message) {
        View view = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            view.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view1 -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View view = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            view.setVisibility(View.GONE);
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
        View lyt_shimmer_default = findViewById(R.id.lyt_shimmer_default);
        View lyt_shimmer_compact = findViewById(R.id.lyt_shimmer_compact);
        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            lyt_shimmer_default.setVisibility(View.GONE);
            lyt_shimmer_compact.setVisibility(View.VISIBLE);
        } else {
            lyt_shimmer_default.setVisibility(View.VISIBLE);
            lyt_shimmer_compact.setVisibility(View.GONE);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_search:
                Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                startActivity(intent);
                return true;

            case R.id.menu_sort:
                String[] items = getResources().getStringArray(R.array.dialog_single_choice_array);
                int itemSelected = sharedPref.getCurrentSortVideos();
                new AlertDialog.Builder(ActivityVideoByCategory.this)
                        .setTitle(R.string.title_sort)
                        .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                            if (callbackCall != null && callbackCall.isExecuted())
                                callbackCall.cancel();
                            adapterVideo.resetListData();
                            requestAction(1);
                            sharedPref.updateSortVideos(position);
                            dialogInterface.dismiss();
                        })
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
