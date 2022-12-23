package com.anime.limma.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anime.limma.R;
import com.anime.limma.adapters.AdapterSuggested;
import com.anime.limma.callbacks.CallbackVideoDetail;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.databases.sqlite.DbFavorite;
import com.anime.limma.models.Video;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.AdsManager;
import com.anime.limma.utils.AppBarLayoutBehavior;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNotificationDetail extends AppCompatActivity {

    private Call<CallbackVideoDetail> callbackCall = null;
    private LinearLayout lytMainContent;
    TextView txtTitle, txtCategory, txtDuration, txtTotalViews, txtDateTime;
    LinearLayout lytView, lytDate;
    ImageView videoThumbnail;
    private WebView webView;
    DbFavorite dbFavorite;
    CoordinatorLayout parentView;
    private ShimmerFrameLayout shimmerFrameLayout;
    RelativeLayout lytSuggested;
    private SwipeRefreshLayout swipeRefreshLayout;
    SharedPref sharedPref;
    ImageButton btnFavorite, btnShare;
    private String videoId;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_video_detail);

        Tools.setNavigation(this);

        tools = new Tools(this);
        sharedPref = new SharedPref(this);
        BannerAdView bannerAdView = (BannerAdView) findViewById(R.id.bannerAdView);
        bannerAdView.setAdUnitId("R-M-1774888-1");
        bannerAdView.setAdSize(AdSize.BANNER_320x50);
        bannerAdView.loadAd(new AdRequest.Builder().build());

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




        dbFavorite = new DbFavorite(getApplicationContext());

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setRefreshing(false);

        lytMainContent = findViewById(R.id.lyt_main_content);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        parentView = findViewById(R.id.lyt_content);

        videoThumbnail = findViewById(R.id.video_thumbnail);
        txtTitle = findViewById(R.id.video_title);
        txtCategory = findViewById(R.id.category_name);
        txtDuration = findViewById(R.id.video_duration);
        webView = findViewById(R.id.video_description);
        txtTotalViews = findViewById(R.id.total_views);
        txtDateTime = findViewById(R.id.date_time);
        lytView = findViewById(R.id.lyt_view_count);
        lytDate = findViewById(R.id.lyt_date);
        btnFavorite = findViewById(R.id.img_favorite);
        btnShare = findViewById(R.id.btn_share);

        lytSuggested = findViewById(R.id.lyt_suggested);

        Intent intent = getIntent();
        videoId = intent.getStringExtra("id");

        requestAction();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            lytMainContent.setVisibility(View.GONE);
            requestAction();
        });

        initToolbar();

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        new Handler().postDelayed(this::requestPostData, 200);
    }

    private void requestPostData() {
        this.callbackCall = RestAdapter.createAPI(sharedPref.getBaseUrl()).getVideoDetail(videoId);
        this.callbackCall.enqueue(new Callback<CallbackVideoDetail>() {
            public void onResponse(Call<CallbackVideoDetail> call, Response<CallbackVideoDetail> response) {
                CallbackVideoDetail responseHome = response.body();
                if (responseHome == null || !responseHome.status.equals("ok")) {
                    onFailRequest();
                    return;
                }
                displayAllData(responseHome);
                swipeProgress(false);
                lytMainContent.setVisibility(View.VISIBLE);
            }

            public void onFailure(Call<CallbackVideoDetail> call, Throwable th) {
                Log.e("onFailure", th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        lytMainContent.setVisibility(View.GONE);
        if (Tools.isConnect(ActivityNotificationDetail.this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed_home);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
            lytMainContent.setVisibility(View.VISIBLE);
            return;
        }
        lytMainContent.setVisibility(View.GONE);
    }

    private void displayAllData(CallbackVideoDetail responseHome) {
        displayData(responseHome.post);
        displaySuggested(responseHome.suggested);
    }

    public void displayData(final Video video) {

        txtTitle.setText(video.video_title);
        txtCategory.setText(video.category_name);
        txtDuration.setText(video.video_duration);

        if (AppConfig.ENABLE_VIEW_COUNT) {
            txtTotalViews.setText(Tools.withSuffix(video.total_views) + " " + getResources().getString(R.string.views_count));
        } else {
            lytView.setVisibility(View.GONE);
        }

        if (AppConfig.ENABLE_DATE_DISPLAY && AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
            txtDateTime.setText(Tools.getFormatedDateSimple(video.date_time));
        } else if (AppConfig.ENABLE_DATE_DISPLAY && !AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
            txtDateTime.setText(Tools.getFormatedDateSimple(video.date_time));
        } else {
            lytDate.setVisibility(View.GONE);
        }

        if (video.video_type != null && video.video_type.equals("youtube")) {
            Picasso.get()
                    .load(Constant.YOUTUBE_IMAGE_FRONT + video.video_id + Constant.YOUTUBE_IMAGE_BACK_HQ)
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(videoThumbnail);
        } else {
            Picasso.get()
                    .load(sharedPref.getBaseUrl() + "/upload/" + video.video_thumbnail)
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(videoThumbnail);
        }

        videoThumbnail.setOnClickListener(view -> {

            if (Tools.isNetworkAvailable(ActivityNotificationDetail.this)) {

                if (video.video_type != null && video.video_type.equals("youtube")) {
                    Intent intent = new Intent(ActivityNotificationDetail.this, ActivityYoutubePlayer.class);
                    intent.putExtra(Constant.KEY_VIDEO_ID, video.video_id);
                    startActivity(intent);
                } else if (video.video_type != null && video.video_type.equals("Upload")) {
                    Intent intent = new Intent(ActivityNotificationDetail.this, ActivityVideoPlayer.class);
                    intent.putExtra("url", sharedPref.getBaseUrl() + "/upload/video/" + video.video_url);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(ActivityNotificationDetail.this, ActivityVideoPlayer.class);
                    intent.putExtra("url", video.video_url);
                    startActivity(intent);
                }

                loadViewed();

            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_required), Toast.LENGTH_SHORT).show();
            }

        });

        Tools.displayPostDescription(this, webView, video.video_description);

        btnShare.setOnClickListener(view -> Tools.shareContent(this, video.video_title, getResources().getString(R.string.share_text)));

        addToFavorite(video);

        new Handler().postDelayed(() -> lytSuggested.setVisibility(View.VISIBLE), 1000);

    }

    private void displaySuggested(List<Video> videos) {

        RecyclerView recyclerView = findViewById(R.id.recycler_view_suggested);
        recyclerView.setLayoutManager(new LinearLayoutManager(ActivityNotificationDetail.this));
        AdapterSuggested adapterSuggested = new AdapterSuggested(ActivityNotificationDetail.this, recyclerView, videos);
        recyclerView.setAdapter(adapterSuggested);
        recyclerView.setNestedScrollingEnabled(false);

        adapterSuggested.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityNotificationDetail.class);
            intent.putExtra("id", obj.vid);
            startActivity(intent);
        });

        adapterSuggested.setOnItemOverflowClickListener((view, obj, position) -> tools.showBottomSheetDialog(ActivityNotificationDetail.this, parentView, obj));

        TextView txtSuggested = findViewById(R.id.txt_suggested);
        if (videos.size() > 0) {
            txtSuggested.setText(getResources().getString(R.string.txt_suggested));
        } else {
            txtSuggested.setText("");
        }

    }

    private void initToolbar() {
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
            getSupportActionBar().setTitle("");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void addToFavorite(Video post) {

        List<Video> data = dbFavorite.getFavRow(videoId);
        if (data.size() == 0) {
            btnFavorite.setImageResource(R.drawable.ic_fav_outline);
        } else {
            if (data.get(0).getVid().equals(videoId)) {
                btnFavorite.setImageResource(R.drawable.ic_fav);
            }
        }

        btnFavorite.setOnClickListener(view -> {
            List<Video> data1 = dbFavorite.getFavRow(videoId);
            if (data1.size() == 0) {
                dbFavorite.addToFavorite(new Video(
                        post.category_name,
                        post.vid,
                        post.video_title,
                        post.video_url,
                        post.video_id,
                        post.video_thumbnail,
                        post.video_duration,
                        post.video_description,
                        post.video_type,
                        post.total_views,
                        post.date_time
                ));
                Snackbar.make(parentView, R.string.msg_favorite_added, Snackbar.LENGTH_SHORT).show();
                btnFavorite.setImageResource(R.drawable.ic_fav);

            } else {
                if (data1.get(0).getVid().equals(videoId)) {
                    dbFavorite.RemoveFav(new Video(videoId));
                    Snackbar.make(parentView, R.string.msg_favorite_removed, Snackbar.LENGTH_SHORT).show();
                    btnFavorite.setImageResource(R.drawable.ic_fav_outline);
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void loadViewed() {
        new MyTask().execute(sharedPref.getBaseUrl() + "/api/get_total_views/?id=" + videoId);
    }

    @SuppressWarnings("deprecation")
    private static class MyTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return Tools.getJSONString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (null == result || result.length() == 0) {
                Log.d("TAG", "no data found!");
            } else {

                try {

                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray("result");
                    JSONObject objJson = null;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        objJson = jsonArray.getJSONObject(i);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void onDestroy() {
        if (!(callbackCall == null || callbackCall.isCanceled())) {
            this.callbackCall.cancel();
        }
        shimmerFrameLayout.stopShimmer();
        super.onDestroy();
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
