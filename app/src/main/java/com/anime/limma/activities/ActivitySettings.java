package com.anime.limma.activities;

import static com.anime.limma.utils.Constant.CATEGORY_GRID_2_COLUMN;
import static com.anime.limma.utils.Constant.CATEGORY_GRID_3_COLUMN;
import static com.anime.limma.utils.Constant.CATEGORY_LIST;
import static com.anime.limma.utils.Constant.VIDEO_LIST_COMPACT;
import static com.anime.limma.utils.Constant.VIDEO_LIST_DEFAULT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.anime.limma.BuildConfig;
import com.anime.limma.R;
import com.anime.limma.databases.prefs.SharedPref;
import com.anime.limma.utils.Tools;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ActivitySettings extends AppCompatActivity {

    private static final String TAG = "ActivitySettings";
    SwitchMaterial switchTheme;
    RelativeLayout btnSwitchTheme;
    SharedPref sharedPref;
    LinearLayout parentView;
    TextView txt_current_video_list;
    TextView txt_current_category_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_settings);
        sharedPref = new SharedPref(this);
        Tools.setNavigation(this);
        initView();
        setupToolbar();
    }

    public void setupToolbar() {
        Tools.setupToolbar(this, findViewById(R.id.toolbar), getString(R.string.title_settings), true);
    }

    private void initView() {
        parentView = findViewById(R.id.parent_view);

        switchTheme = findViewById(R.id.switch_theme);
        switchTheme.setChecked(sharedPref.getIsDarkTheme());
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.e("INFO", "" + isChecked);
            sharedPref.setIsDarkTheme(isChecked);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnSwitchTheme = findViewById(R.id.btn_switch_theme);
        btnSwitchTheme.setOnClickListener(v -> {
            if (switchTheme.isChecked()) {
                sharedPref.setIsDarkTheme(false);
                switchTheme.setChecked(false);
            } else {
                sharedPref.setIsDarkTheme(true);
                switchTheme.setChecked(true);
            }
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }, 200);
        });

        changeVideoListViewType();
        changeCategoryListViewType();

        findViewById(R.id.btn_notification).setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
        });

        findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPrivacyPolicy.class)));

        findViewById(R.id.btn_share).setOnClickListener(view -> Tools.shareApp(ActivitySettings.this, getString(R.string.share_text)));

        findViewById(R.id.btn_rate).setOnClickListener(v -> Tools.rateUs(ActivitySettings.this));

        findViewById(R.id.btn_more).setOnClickListener(v -> Tools.moreApps(ActivitySettings.this, sharedPref.getMoreAppsUrl()));

        findViewById(R.id.btn_about).setOnClickListener(v -> Tools.showAboutDialog(ActivitySettings.this));

    }

    private void changeVideoListViewType() {

        txt_current_video_list = findViewById(R.id.txt_current_video_list);
        if (sharedPref.getVideoViewType() == VIDEO_LIST_DEFAULT) {
            txt_current_video_list.setText(getResources().getString(R.string.single_choice_default));
        } else if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
            txt_current_video_list.setText(getResources().getString(R.string.single_choice_compact));
        }

        findViewById(R.id.btn_switch_list).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_video_list);
            int itemSelected = sharedPref.getVideoViewType();
            new AlertDialog.Builder(ActivitySettings.this)
                    .setTitle(R.string.title_settings_videos)
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                        sharedPref.updateVideoViewType(position);

                        if (position == 0) {
                            txt_current_video_list.setText(getResources().getString(R.string.single_choice_default));
                        } else if (position == 1) {
                            txt_current_video_list.setText(getResources().getString(R.string.single_choice_compact));
                        }

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    private void changeCategoryListViewType() {

        txt_current_category_list = findViewById(R.id.txt_current_category_list);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_list));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_2));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_3));
        }

        findViewById(R.id.btn_switch_category).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_category_list);
            int itemSelected = sharedPref.getCategoryViewType();
            new AlertDialog.Builder(ActivitySettings.this)
                    .setTitle(R.string.title_settings_category)
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                        sharedPref.updateCategoryViewType(position);

                        if (position == 0) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_list));
                        } else if (position == 1) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_2));
                        } else if (position == 2) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_3));
                        }

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("category_position", "category_position");
                        startActivity(intent);

                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
