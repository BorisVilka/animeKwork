package com.anime.limma.utils;

import static com.anime.limma.config.AppConfig.USE_LEGACY_GDPR_EU_CONSENT;

import android.app.Activity;
import android.view.View;

import com.anime.limma.BuildConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.databases.prefs.SharedPref;

public class AdsManager {

    Activity activity;
    SharedPref sharedPref;
    AdsPref adsPref;

    public AdsManager(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
        this.adsPref = new AdsPref(activity);

    }


    public void showInterstitialAd() {

    }



}
