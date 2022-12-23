package com.anime.limma.activities;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.anime.limma.callbacks.CallbackSettings;
import com.anime.limma.config.AppConfig;
import com.anime.limma.databases.prefs.AdsPref;
import com.anime.limma.models.Settings;
import com.anime.limma.rests.RestAdapter;
import com.anime.limma.utils.Tools;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    Activity activity;
    public static final String TAG = "MyApplication";
    private static MyApplication mInstance;
    String message = "";
    String big_picture = "";
    String title = "";
    String link = "";
    long post_id = -1;
    long unique_id = -1;
    FirebaseAnalytics mFirebaseAnalytics;
    Activity currentActivity;
    AdsPref adsPref;
    Settings settings;

    public MyApplication() {
        mInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        com.yandex.mobile.ads.common.MobileAds.initialize(this, () -> {

        });
        FirebaseApp.initializeApp(this);
        adsPref = new AdsPref(this);
        //MobileAds.initialize(this, initializationStatus -> {
        //});
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mInstance = this;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        OneSignal.disablePush(false);
        Log.d(TAG, "OneSignal Notification is enabled");

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        requestTopic();

        OneSignal.setNotificationOpenedHandler(
                result -> {
                    title = result.getNotification().getTitle();
                    message = result.getNotification().getBody();
                    big_picture = result.getNotification().getBigPicture();
                    Log.d(TAG, title + ", " + message + ", " + big_picture);
                    try {
                        unique_id = result.getNotification().getAdditionalData().getLong("unique_id");
                        post_id = result.getNotification().getAdditionalData().getLong("post_id");
                        link = result.getNotification().getAdditionalData().getString("link");
                        Log.d(TAG, post_id + ", " + unique_id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("unique_id", unique_id);
                    intent.putExtra("post_id", post_id);
                    intent.putExtra("title", title);
                    intent.putExtra("link", link);
                    startActivity(intent);
                });

        OneSignal.unsubscribeWhenNotificationsAreDisabled(true);
    }

    private void requestTopic() {
        String data = Tools.decode(AppConfig.SERVER_KEY);
        String[] results = data.split("_applicationId_");
        String baseUrl = results[0].replace("http://localhost", "http://10.0.2.2");

        Call<CallbackSettings> callbackCall = RestAdapter.createAPI(baseUrl).getSettings(AppConfig.REST_API_KEY);
        callbackCall.enqueue(new Callback<CallbackSettings>() {
            public void onResponse(Call<CallbackSettings> call, Response<CallbackSettings> response) {
                CallbackSettings resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    settings = resp.settings;
                    FirebaseMessaging.getInstance().subscribeToTopic(settings.fcm_notification_topic);
                    OneSignal.setAppId(settings.onesignal_app_id);
                    Log.d(TAG, "FCM Subscribe topic : " + settings.fcm_notification_topic);
                    Log.d(TAG, "OneSignal App ID : " + settings.onesignal_app_id);
                }
            }

            public void onFailure(Call<CallbackSettings> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }


}
