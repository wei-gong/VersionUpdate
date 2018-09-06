package com.qtec.versionupdate;

import java.io.File;

/**
 * Created by gongw on 2018/9/5.
 */

public interface DownloadCallback {

    void onFailed(Throwable throwable);

    void onProgress(int progress);

    void onSuccess(File file);

}
