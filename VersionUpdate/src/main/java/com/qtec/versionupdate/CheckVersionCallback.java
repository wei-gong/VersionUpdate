package com.qtec.versionupdate;

import java.io.File;

/**
 * Created by gongw on 2018/9/4.
 */

public interface CheckVersionCallback {

    void onStartCheck();

    void onError(Throwable throwable);

    void onNoNewVersionChecked(VersionInfo versionInfo);

    void onNetNewVersionChecked(VersionInfo versionInfo);

    void onLocalNewVersionChecked(VersionInfo versionInfo, File file);

}
