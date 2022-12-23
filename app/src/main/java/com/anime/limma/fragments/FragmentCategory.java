package com.anime.limma.fragments;

import static com.anime.limma.utils.Constant.CATEGORY_GRID_2_COLUMN;
import static com.anime.limma.utils.Constant.CATEGORY_GRID_3_COLUMN;
import static com.anime.limma.utils.Constant.CATEGORY_LIST;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anime.limma.R;
import com.anime.limma.activities.ActivityVideoByCategory;
import com.anime.limma.activities.MainActivity;
import com.anime.limma.adapters.AdapterCategory;
import com.anime.limma.callbacks.CallbackCategories;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.models.Category;
import com.anime.limma.rests.ApiInterface;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.EqualSpacingItemDecoration;
import com.anime.limma.utils.ItemOffsetDecoration;
import com.anime.limma.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterCategory adapterCategory;
    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    private Call<CallbackCategories> callbackCall = null;
    private ShimmerFrameLayout shimmerFrameLayout;
    SharedPref sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_category, container, false);

        if (getActivity() != null)
            sharedPref = new SharedPref(getActivity());

        shimmerFrameLayout = rootView.findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout_category);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = rootView.findViewById(R.id.recyclerViewCategory);
        recyclerView.setHasFixedSize(true);

        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.item_offset);

        if (sharedPref.getCategoryViewType() == CATEGORY_LIST) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, LinearLayoutManager.VERTICAL));
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.spacing_small), 0, 0);
            recyclerView.addItemDecoration(new EqualSpacingItemDecoration(0));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
            recyclerView.addItemDecoration(itemDecoration);
        }
        if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL));
            recyclerView.addItemDecoration(itemDecoration);
        }

        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterCategory = new AdapterCategory(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityVideoByCategory.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            ((MainActivity) getActivity()).showInterstitialAd();
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapterCategory.resetListData();
            requestAction();
        });

        requestAction();
        initShimmerLayout();

        return rootView;
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.setListData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestCategoriesApi() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getAllCategories(AppConfig.REST_API_KEY);
        callbackCall.enqueue(new Callback<CallbackCategories>() {
            @Override
            public void onResponse(Call<CallbackCategories> call, Response<CallbackCategories> response) {
                CallbackCategories resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    displayApiResult(resp.categories);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackCategories> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler().postDelayed(() -> requestCategoriesApi(), Constant.DELAY_TIME);
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

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = rootView.findViewById(R.id.lyt_failed_category);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item_category);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
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
        View lyt_shimmer_category_list = rootView.findViewById(R.id.lyt_shimmer_category_list);
        View lyt_shimmer_category_grid2 = rootView.findViewById(R.id.lyt_shimmer_category_grid2);
        View lyt_shimmer_category_grid3 = rootView.findViewById(R.id.lyt_shimmer_category_grid3);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST) {
            lyt_shimmer_category_list.setVisibility(View.VISIBLE);
            lyt_shimmer_category_grid2.setVisibility(View.GONE);
            lyt_shimmer_category_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            lyt_shimmer_category_list.setVisibility(View.GONE);
            lyt_shimmer_category_grid2.setVisibility(View.VISIBLE);
            lyt_shimmer_category_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            lyt_shimmer_category_list.setVisibility(View.GONE);
            lyt_shimmer_category_grid2.setVisibility(View.GONE);
            lyt_shimmer_category_grid3.setVisibility(View.VISIBLE);
        }
    }

}
