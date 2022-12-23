package com.anime.limma.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.anime.limma.R;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.databases.sqlite.DbFavorite;
import com.anime.limma.models.Video;
import com.anime.limma.utils.AppBarLayoutBehavior;
import com.anime.limma.utils.Constant;
import com.anime.limma.utils.Tools;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ActivityVideoDetailOffline extends AppCompatActivity {

    String strCategory, strVid, strTitle, strUrl, strVideoId, strThumbnail, strDuration, strDescription, strType, strDateTime;
    String strCid;
    long totalViews;
    ImageView videoThumbnail;
    TextView txtTitle, txtCategory, txtDuration, txtTotalViews, txtDateTime;
    LinearLayout lytView, lytDate;
    WebView webView;
    Snackbar snackbar;
    ImageButton btnFavorite, btnShare;
    DbFavorite dbFavorite;
    SharedPref sharedPref;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_video_detail_offline);

        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        dbFavorite = new DbFavorite(getApplicationContext());

        Intent intent = getIntent();
        if (null != intent) {
            position = intent.getIntExtra(Constant.POSITION, 0);
            strCid = intent.getStringExtra(Constant.KEY_VIDEO_CATEGORY_ID);
            strCategory = intent.getStringExtra(Constant.KEY_VIDEO_CATEGORY_NAME);
            strVid = intent.getStringExtra(Constant.KEY_VID);
            strTitle = intent.getStringExtra(Constant.KEY_VIDEO_TITLE);
            strUrl = intent.getStringExtra(Constant.KEY_VIDEO_URL);
            strVideoId = intent.getStringExtra(Constant.KEY_VIDEO_ID);
            strThumbnail = intent.getStringExtra(Constant.KEY_VIDEO_THUMBNAIL);
            strDuration = intent.getStringExtra(Constant.KEY_VIDEO_DURATION);
            strDescription = intent.getStringExtra(Constant.KEY_VIDEO_DESCRIPTION);
            strType = intent.getStringExtra(Constant.KEY_VIDEO_TYPE);
            totalViews = intent.getLongExtra(Constant.KEY_TOTAL_VIEWS, 0);
            strDateTime = intent.getStringExtra(Constant.KEY_DATE_TIME);
        }

        setupToolbar();

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

        displayData();
        addFavorite();

    }

    private void setupToolbar() {
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
            getSupportActionBar().setTitle(strCategory);
        }
    }

    public void displayData() {

        txtTitle.setText(strTitle);
        txtCategory.setText(strCategory);
        txtDuration.setText(strDuration);

        if (AppConfig.ENABLE_VIEW_COUNT) {
            txtTotalViews.setText(Tools.withSuffix(totalViews) + " " + getResources().getString(R.string.views_count));
        } else {
            lytView.setVisibility(View.GONE);
        }

        if (AppConfig.ENABLE_DATE_DISPLAY && AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
            txtDateTime.setText(Tools.getTimeAgo(strDateTime));
        } else if (AppConfig.ENABLE_DATE_DISPLAY && !AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
            txtDateTime.setText(Tools.getFormatedDateSimple(strDateTime));
        } else {
            lytDate.setVisibility(View.GONE);
        }


        if (strType != null && strType.equals("youtube")) {
            Picasso.get()
                    .load(Constant.YOUTUBE_IMAGE_FRONT + strVideoId + Constant.YOUTUBE_IMAGE_BACK_HQ)
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(videoThumbnail);
        } else {
            Picasso.get()
                    .load(sharedPref.getBaseUrl() + "/upload/" + strThumbnail)
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(videoThumbnail);
        }

        videoThumbnail.setOnClickListener(view -> {

            if (Tools.isNetworkAvailable(ActivityVideoDetailOffline.this)) {

                if (strType != null && strType.equals("youtube")) {
                    Intent intent = new Intent(ActivityVideoDetailOffline.this, ActivityYoutubePlayer.class);
                    intent.putExtra(Constant.KEY_VIDEO_ID, strVideoId);
                    startActivity(intent);
                } else if (strType != null && strType.equals("Upload")) {
                    Intent intent = new Intent(ActivityVideoDetailOffline.this, ActivityVideoPlayer.class);
                    intent.putExtra("url", sharedPref.getBaseUrl() + "/upload/video/" + strUrl);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(ActivityVideoDetailOffline.this, ActivityVideoPlayer.class);
                    intent.putExtra("url", strUrl);
                    startActivity(intent);
                }

                loadViewed();

            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_required), Toast.LENGTH_SHORT).show();
            }

        });

        Tools.displayPostDescription(this, webView, strDescription);

        btnShare.setOnClickListener(view -> Tools.shareContent(this, strTitle, getResources().getString(R.string.share_text)));

    }

    private void loadViewed() {
        new MyTask().execute(sharedPref.getBaseUrl() + "/api/get_total_views/?id=" + strVid);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
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

    public void addFavorite() {

        List<Video> data = dbFavorite.getFavRow(strVid);
        if (data.size() == 0) {
            btnFavorite.setImageResource(R.drawable.ic_fav_outline);
        } else {
            if (data.get(0).getVid().equals(strVid)) {
                btnFavorite.setImageResource(R.drawable.ic_fav);
            }
        }

        btnFavorite.setOnClickListener(view -> {
            List<Video> data1 = dbFavorite.getFavRow(strVid);
            if (data1.size() == 0) {
                dbFavorite.addToFavorite(new Video(
                        strCategory,
                        strVid,
                        strTitle,
                        strUrl,
                        strVideoId,
                        strThumbnail,
                        strDuration,
                        strDescription,
                        strType,
                        totalViews,
                        strDateTime
                ));
                snackbar = Snackbar.make(view, getResources().getString(R.string.msg_favorite_added), Snackbar.LENGTH_SHORT);
                snackbar.show();

                btnFavorite.setImageResource(R.drawable.ic_fav);

            } else {
                if (data1.get(0).getVid().equals(strVid)) {
                    dbFavorite.RemoveFav(new Video(strVid));
                    snackbar = Snackbar.make(view, getResources().getString(R.string.msg_favorite_removed), Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    btnFavorite.setImageResource(R.drawable.ic_fav_outline);
                }
            }
        });

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
