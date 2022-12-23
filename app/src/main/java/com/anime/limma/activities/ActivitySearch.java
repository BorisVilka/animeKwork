package com.anime.limma.activities;

import static com.anime.limma.utils.Constant.VIDEO_LIST_COMPACT;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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

import com.anime.limma.R;
import com.anime.limma.adapters.AdapterVideo;
import com.anime.limma.callbacks.CallbackListVideo;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.rests.ApiInterface;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.AdsManager;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySearch extends AppCompatActivity {

    private EditText et_search;
    private RecyclerView recyclerView;
    private AdapterVideo mAdapterSearch;
    private ImageButton bt_clear;
    private Call<CallbackListVideo> callbackCall = null;
    SharedPref sharedPref;
    private ShimmerFrameLayout lyt_shimmer;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;
    CoordinatorLayout parentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_search);

        Tools.setNavigation(this);

        tools = new Tools(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        BannerAdView bannerAdView = (BannerAdView) findViewById(R.id.bannerAdView);
        bannerAdView.setAdUnitId("R-M-1774888-1");
        bannerAdView.setAdSize(AdSize.BANNER_320x50);
        bannerAdView.loadAd(new AdRequest.Builder().build());

        InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId("R-M--3");
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



        parentView = findViewById(R.id.parent_view);
        et_search = findViewById(R.id.et_search);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setVisibility(View.GONE);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        swipeProgress(false);

        recyclerView = findViewById(R.id.recyclerView);
        if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.spacing_small), 0, 0);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        et_search.addTextChangedListener(textWatcher);

        //set data and list mAdapterSearch
        mAdapterSearch = new AdapterVideo(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(mAdapterSearch);

        bt_clear.setOnClickListener(view -> et_search.setText(""));

        et_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchAction();
                return true;
            }
            return false;
        });

        mAdapterSearch.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityVideoDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
        });

        mAdapterSearch.setOnItemOverflowClickListener((view, obj, position) -> tools.showBottomSheetDialog(ActivitySearch.this, parentView, obj));

        setupToolbar();
        initShimmerLayout();

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
            getSupportActionBar().setTitle("");
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchApi(final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getSearchPosts(query, Constant.MAX_SEARCH_RESULT, AppConfig.REST_API_KEY);
        callbackCall.enqueue(new Callback<CallbackListVideo>() {
            @Override
            public void onResponse(Call<CallbackListVideo> call, Response<CallbackListVideo> response) {
                CallbackListVideo resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    mAdapterSearch.insertDataWithNativeAd(resp.posts);
                    if (resp.posts.size() == 0) showNotFoundView(true);
                } else {
                    onFailRequest();
                }
                swipeProgress(false);
            }

            @Override
            public void onFailure(Call<CallbackListVideo> call, Throwable t) {
                onFailRequest();
                swipeProgress(false);
            }

        });
    }

    private void onFailRequest() {
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.no_internet_text));
        }
    }

    private void searchAction() {
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            mAdapterSearch.resetListData();
            swipeProgress(true);
            new Handler().postDelayed(() -> requestSearchApi(query), Constant.DELAY_TIME);
        } else {
            Toast.makeText(getApplicationContext(),  getResources().getString(R.string.msg_search_input), Toast.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchAction());
    }

    private void showNotFoundView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_post_found);
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
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        } else {
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        }
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
    public void onBackPressed() {
        if (et_search.length() > 0) {
            et_search.setText("");
        } else {
            super.onBackPressed();
        }
    }

}
