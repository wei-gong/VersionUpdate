package com.qtec.versionupdate;


import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

/**
 * Created by gongw on 2018/9/4.
 * Version information on the update server
 */
public class VersionInfo implements Serializable{
    private static final long serialVersionUID = 1L;
    /**
     * version code, used to check whether it is the latest version
     */
    protected int versionCode;
    /**
     * version name
     */
    protected String versionName;
    /**
     * url where you can download the apk
     */
    protected String apkUrl;
    /**
     * detail description of this version
     */
    protected String description;
    /**
     * md5 hex string of apk file
     */
    protected String md5;

    public VersionInfo(){}

    public VersionInfo(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            this.versionCode = jsonObject.getInt("versionCode");
            this.versionName = jsonObject.getString("versionName");
            this.apkUrl = jsonObject.getString("apkUrl");
            this.description = jsonObject.getString("description");
            this.md5 = jsonObject.getString("md5");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toJsonString(){
       return "{\"versionCode\":" + versionCode + ","
               +"\"versionName\":\"" + versionName + "\","
               +"\"apkUrl\":\"" + apkUrl + "\","
               +"\"description\":\"" + description + "\","
               +"\"md5\":\"" + md5 + "\"}";
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
