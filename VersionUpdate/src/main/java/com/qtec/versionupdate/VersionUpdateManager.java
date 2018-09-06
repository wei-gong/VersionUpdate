package com.qtec.versionupdate;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firstly, generate your version info file by {@link VersionInfo#toJsonString()} and {@link #md5Hex(File)},
 * and set your updateUrl where you can get the version info. Then you can start version update by {@link #checkVersion(Context, CheckVersionCallback)}
 * or {@link #update(Context, boolean)}.
 * Created by gongw on 2018/9/4.
 */
public class VersionUpdateManager {
    /**
     * handler of ui thread
     */
    private Handler handler = new Handler(Looper.getMainLooper());
    /**
     * url to get version info file which contains {@link VersionInfo#toJsonString()}
     */
    private String updateUrl;
    /**
     * local path for apk file
     */
    private String storeDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/VersionUpdate";
    /**
     * latest apkFile downloaded from server
     */
    private File apkFile;
    private ExecutorService executors = Executors.newSingleThreadExecutor();

    private VersionUpdateManager(){
        File file = new File(storeDirectory);
        if(!file.exists()){
            file.mkdirs();
        }
    }

    private static class Holder{
        private static VersionUpdateManager mInstance = new VersionUpdateManager();
    }

    public static VersionUpdateManager getInstance() {
        return Holder.mInstance;
    }

    /**
     * set server url where update_info.txt stored
     * @param url a url
     */
    public void setUpdateUrl(String url){
        this.updateUrl = url;
    }


    public void update(Context context, boolean showCheckingIndicator){
        Intent intent = new Intent("com.qtec.version_update");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(UpdateActivity.SHOW_CHECKING_INDICATOR, showCheckingIndicator);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * check if there is a new version on the update server
     * @param checkVersionCallback call back on android main thread
     */
    public void checkVersion(Context context, CheckVersionCallback checkVersionCallback){
        executors.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (checkVersionCallback != null) {
                        checkVersionCallback.onStartCheck();
                    }
                    HttpURLConnection connection = VersionUpdateManager.this.getUrlConnection(updateUrl);
                    InputStream is = connection.getInputStream();
                    VersionInfo versionInfo = new VersionInfo(VersionUpdateManager.this.readToString(is, "utf-8"));
                    is.close();
                    PackageManager packageManager = context.getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                    int currentVersion = packageInfo.versionCode;
                    if (versionInfo.getVersionCode() > currentVersion) {
                        if (VersionUpdateManager.this.checkIfDownloaded(packageManager, versionInfo)) {
                            if (checkVersionCallback != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkVersionCallback.onLocalNewVersionChecked(versionInfo, apkFile);
                                    }
                                });
                            }
                        } else {
                            if (checkVersionCallback != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkVersionCallback.onNetNewVersionChecked(versionInfo);
                                    }
                                });
                            }
                        }
                    } else {
                        if (checkVersionCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    checkVersionCallback.onNoNewVersionChecked(versionInfo);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (checkVersionCallback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                checkVersionCallback.onError(e);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Check whether there is a apk file at local store directory, which version code and md5 all matched
     * @param versionInfo versionInfo contains newest version information from server
     * @return true: there is a matched apk file ,handle it with {@link #apkFile}
     */
    private boolean checkIfDownloaded(PackageManager packageManager, VersionInfo versionInfo) {
        int newVersionCode = versionInfo.getVersionCode();
        File directory = new File(storeDirectory);
        File[] listFiles = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return false;
                }
                try {
                    PackageInfo packageArchiveInfo = packageManager.getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    //check version code
                    return packageArchiveInfo.versionCode == newVersionCode;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        if(listFiles == null || listFiles.length <= 0 ){
            return false;
        }
        for(File file : listFiles){
            //check md5
            String md5Hex = md5Hex(file);
            if(md5Hex != null && md5Hex.equals(versionInfo.getMd5())){
                apkFile = file;
                return true;
            }
        }
        return false;
    }

    /**
     * get md5 hex string of file
     * @param file file
     * @return md5 hex string or null
     */
    private String md5Hex(File file){
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length;
            while((length = fis.read(buffer)) != -1){
                md.update(buffer, 0, length);
            }
            fis.close();
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * download file from url
     * @param url
     * @param callback
     */
    public void download(String url, DownloadCallback callback){
        executors.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    HttpURLConnection connection = VersionUpdateManager.this.getUrlConnection(url);
                    is = connection.getInputStream();
                    long total = connection.getContentLength();
                    File file = new File(storeDirectory, url.substring(url.lastIndexOf("/") + 1));
                    fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    int sum = 0;
                    while ((length = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, length);
                        sum += length;
                        int progress = (int) (sum * 100f / total);
                        if (callback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onProgress(progress);
                                }
                            });
                        }
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(file);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailed(e);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * try to install apk, for android version upon 7.0, must add your store path to res/xml/file_paths.xml
     * @param file apk file
     */
    public void installApk(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 兼容7.0+以上版本
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName()+".fileProvider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    /**
     * get url connection, use Get request method
     * @param url
     * @return
     * @throws IOException
     */
    private HttpURLConnection getUrlConnection(String url) throws IOException {
        URL downloadUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.connect();
        return connection;
    }

    /**
     * read inputStream to string
     * @param is inputStream
     * @param charset charset of string
     * @return string of inputStream
     */
    private String readToString(InputStream is, String charset){
        ByteArrayOutputStream result = null;
        try {
            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            result.close();
            return result.toString(charset);
        } catch (Exception e) {
            e.printStackTrace();
            if(result != null){
                try {
                    result.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * set local path for storing apk
     * @param path local path for apk
     */
    public void setStoreDirectory(String path){
        File file = new File(path);
        if(!file.exists() && !file.mkdirs()){
            throw new IllegalArgumentException(path +"can not create");
        }
        if(file.isFile()){
            throw new IllegalArgumentException(path +" is not a directory");
        }
        this.storeDirectory = path;
    }

    /**
     * delete all files stored at sotrePath
     */
    public void clearStore(){
        File file = new File(storeDirectory);
        if(!file.exists() || file.isFile()){
            return;
        }
        File[] fileList = file.listFiles();
        for(File f : fileList){
            if(f.isFile()){
                file.delete();
            }
        }
    }

}
