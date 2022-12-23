package com.anime.limma.activities;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.anime.limma.BuildConfig;
import com.anime.limma.R;
import com.anime.limma.callbacks.CallbackSettings;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.models.Ads;
import com.anime.limma.models.Settings;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.Tools;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    ProgressBar progressBar;
    ImageView img_splash;
    SharedPref sharedPref;
    AdsPref adsPref;
    Ads ads;
    Settings settings;
    Call<CallbackSettings> callbackCall = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_splash);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        img_splash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            img_splash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            img_splash.setImageResource(R.drawable.bg_splash_default);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        requestConfig();

    }

    private void requestConfig() {
        String data = Tools.decode(AppConfig.SERVER_KEY);
        String[] results = data.split("_applicationId_");
        String baseUrl = results[0].replace("http://localhost", "http://10.0.2.2");
        String applicationId = results[1];
        sharedPref.setBaseUrl(baseUrl);

        if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
            if (Tools.isConnect(this)) {
                requestAds(baseUrl);
            } else {
                startMainActivity();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Whoops! invalid server key or applicationId, please check your configuration")
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private void requestAds(String apiUrl) {
        this.callbackCall = RestAdapter.createAPI(apiUrl).getSettings(AppConfig.REST_API_KEY);
        this.callbackCall.enqueue(new Callback<CallbackSettings>() {
            public void onResponse(Call<CallbackSettings> call, Response<CallbackSettings> response) {
                CallbackSettings resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    ads = resp.ads;
                    settings = resp.settings;
                    adsPref.saveAds(
                            ads.ad_status.replace("on", "1"),
                            ads.ad_type,
                            ads.backup_ads,
                            ads.admob_publisher_id,
                            ads.admob_app_id,
                            ads.admob_banner_unit_id,
                            ads.admob_interstitial_unit_id,
                            ads.admob_native_unit_id,
                            ads.admob_app_open_ad_unit_id,
                            ads.unity_game_id,
                            ads.unity_banner_placement_id,
                            ads.unity_interstitial_placement_id,
                            ads.applovin_banner_ad_unit_id,
                            ads.applovin_interstitial_ad_unit_id,
                            ads.applovin_native_ad_manual_unit_id,
                            ads.applovin_banner_zone_id,
                            ads.applovin_interstitial_zone_id,
                            ads.interstitial_ad_interval,
                            ads.native_ad_interval,
                            ads.native_ad_index
                    );

                    sharedPref.saveConfig(
                            resp.key,
                            settings.more_apps_url,
                            settings.privacy_policy
                    );

                }
                startMainActivity();
            }

            public void onFailure(Call<CallbackSettings> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                startMainActivity();
            }
        });
    }

    private void startMainActivity() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }

}
