package com.qtec.versionupdate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.File;

/**
 * this is a translucent activity, get VersionUpdateManager instance by {@link VersionUpdateManager#getInstance()},
 * invoke {@link VersionUpdateManager#checkVersion(Context, CheckVersionCallback)} to start update
 * Created by gongw on 22/11/2017.
 */

public class UpdateActivity extends AppCompatActivity {

    public static final String SHOW_CHECKING_INDICATOR = "show_checking_indicator";
    private ProgressBar progressBar;
    private AlertDialog updateMsgDialog;
    private ProgressDialog updateProgressDialog;
    private boolean showCheckingIndicator;

    private CheckVersionCallback checkVersionCallback = new CheckVersionCallback() {
        @Override
        public void onStartCheck() {
            if(showCheckingIndicator){
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            progressBar.setVisibility(View.GONE);
            if(showCheckingIndicator){
                Toast.makeText(UpdateActivity.this, R.string.check_version_failed, Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        @Override
        public void onNoNewVersionChecked(VersionInfo versionInfo) {
            progressBar.setVisibility(View.GONE);
            if(showCheckingIndicator){
                Toast.makeText(UpdateActivity.this, R.string.already_latest_version, Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        @Override
        public void onNetNewVersionChecked(VersionInfo versionInfo) {
            progressBar.setVisibility(View.GONE);
            showUpdateMsgDialog(versionInfo, null);
        }

        @Override
        public void onLocalNewVersionChecked(VersionInfo versionInfo, File file) {
            progressBar.setVisibility(View.GONE);
            showUpdateMsgDialog(versionInfo, file);
        }
    };

    private DownloadCallback downloadCallback = new DownloadCallback() {
        @Override
        public void onFailed(Throwable throwable) {
                Toast.makeText(UpdateActivity.this, R.string.check_network_network_is_not_user, Toast.LENGTH_SHORT).show();
                finish();
        }

        @Override
        public void onProgress(int progress) {
             showUpdateProgressDialog(progress);
        }

        @Override
        public void onSuccess(File file) {
            VersionUpdateManager.getInstance().installApk(UpdateActivity.this, file);
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showCheckingIndicator = getIntent().getBooleanExtra(SHOW_CHECKING_INDICATOR, false);
        initContentView();
        VersionUpdateManager.getInstance().checkVersion(this, checkVersionCallback);
    }

    /**
     * set content view and init progress bar
     */
    private void initContentView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setGravity(Gravity.CENTER);
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        layout.addView(progressBar);
        setContentView(layout);
    }

    /**
     * show alert dialog filled with versionInfo, set button click listener
     */
    private void showUpdateMsgDialog(VersionInfo versionInfo, File localFile){
        if(updateMsgDialog == null){
            updateMsgDialog = new AlertDialog.Builder(this).create();
            updateMsgDialog.setCancelable(false);
            updateMsgDialog.setCanceledOnTouchOutside(false);
        }
        if(localFile == null){
            updateMsgDialog.setTitle(getString(R.string.new_version_checked)+ versionInfo.getVersionName());
            updateMsgDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.download_and_install), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VersionUpdateManager.getInstance().download(versionInfo.getApkUrl(), downloadCallback);
                }
            });
        }else{
            updateMsgDialog.setTitle(getString(R.string.new_version_downloaded)+ versionInfo.getVersionName());
            updateMsgDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.install_right_now), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VersionUpdateManager.getInstance().installApk(UpdateActivity.this, localFile);
                    finish();
                }
            });
        }
        updateMsgDialog.setMessage(versionInfo.getDescription());
        updateMsgDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UpdateActivity.this.finish();
            }
        });
        updateMsgDialog.show();
    }

    /**
     * show progress dialog of downloading apk
     */
    private void showUpdateProgressDialog(int progress){
        if(updateProgressDialog == null){
            updateProgressDialog = new ProgressDialog(this);
            updateProgressDialog.setTitle(R.string.downloading);
            updateProgressDialog.setIndeterminate(false);
            updateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            updateProgressDialog.setCanceledOnTouchOutside(false);
            updateProgressDialog.setCancelable(false);
        }
        updateProgressDialog.setProgress(progress);
        updateProgressDialog.show();
    }

}
