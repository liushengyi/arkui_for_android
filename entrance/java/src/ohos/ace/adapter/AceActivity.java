/*
 * Copyright (c) 2022 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ohos.ace.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import ohos.ace.adapter.capability.grantresult.GrantResult;
import ohos.ace.adapter.capability.video.AceVideoPluginAosp;

/**
 * A base class for the Ability Cross-platform Environment(ACE) to run on
 * Android. This class is inherited from
 * android Activity. It is entrance of life-cycles of android applications.
 *
 * @since 1
 */
public class AceActivity extends Activity {
    private static final String LOG_TAG = "AceActivity";

    /**
     * web like js app version
     */
    public static final int VERSION_JS = 1;

    /**
     * declarative ets app version
     */
    public static final int VERSION_ETS = 2;

    private static final String ASSET_PATH = "js/";

    private static final String INSTANCE_DEFAULT_NAME = "default";

    private static final String ASSET_PATH_SHARE = "share";

    private static final String COLOR_MODE_KEY = "colorMode";

    private static final String COLOR_MODE_LIGHT = "light";

    private static final String COLOR_MODE_DARK = "dark";

    private static final String ORI_MODE_KEY = "orientation";

    private static final String ORI_MODE_LANDSCAPE = "LANDSCAPE";

    private static final String ORI_MODE_PORTRAIT = "PORTRAIT";

    private static final String DENSITY_KEY = "densityDpi";

    private static final int THEME_ID_LIGHT = 117440515;

    private static final int THEME_ID_DARK = 117440516;

    private static final int GRAY_THRESHOLD = 255;

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    private String instanceName;

    private int activityId = ID_GENERATOR.getAndIncrement();

    private int widthPixels = 0;

    private int heightPixels = 0;

    private int version = 1;

    private float density = 1.0f;

    private AceContainer container = null;

    private AceViewCreatorAosp viewCreator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ALog.i(LOG_TAG, "AceActivity onCreate called");
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // for 2.0 version, use DECLARATIVE_JS container type
        if (version == VERSION_ETS) {
            AceEnv.setContainerType(AceContainer.CONTAINER_TYPE_DECLARATIVE_JS);
        }
        viewCreator = new AceViewCreatorAosp(this);
        AceEnv.setViewCreator(viewCreator);
        AceEnv.getInstance().setupFirstFrameHandler(AceEnv.ACE_PLATFORM_ANDROID);
        boolean isDebug = (context.getApplicationInfo() != null)
                && ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);

        AceApplicationInfo.getInstance().setPackageInfo(getPackageName(), getUid(this), isDebug, false);
        AceApplicationInfo.getInstance().setPid(Process.myPid());
        AceApplicationInfo.getInstance().setLocale();
        createContainer();
    }

    @Override
    protected void onStart() {
        ALog.i(LOG_TAG, "AceActivity onStart called");
        super.onStart();
    }

    @Override
    protected void onResume() {
        ALog.i(LOG_TAG, "AceActivity onResume called");
        super.onResume();
        if (container == null) {
            ALog.w(LOG_TAG, "onResume container is null");
            return;
        }
        IAceView aceView = container.getView(density, widthPixels, heightPixels);
        aceView.onShow();
        container.onShow();
    }

    @Override
    protected void onRestart() {
        ALog.i(LOG_TAG, "AceActivity onRestart called");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        ALog.i(LOG_TAG, "AceActivity onStop called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ALog.i(LOG_TAG, "AceActivity onDestroy called");
        super.onDestroy();
        AceEnv.destroyContainer(container);
    }

    @Override
    public void onBackPressed() {
        ALog.i(LOG_TAG, "AceActivity onBackPressed called");
        if (container != null && !container.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            JSONObject json = new JSONObject();
            int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    container.setColorMode(AceContainer.COLOR_MODE_LIGHT);
                    json.put(COLOR_MODE_KEY, COLOR_MODE_LIGHT);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    container.setColorMode(AceContainer.COLOR_MODE_DARK);
                    json.put(COLOR_MODE_KEY, COLOR_MODE_DARK);
                    break;
                default:
                    break;
            }
            int orientationMode = newConfig.orientation;
            switch (orientationMode) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    json.put(ORI_MODE_KEY, ORI_MODE_LANDSCAPE);
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    json.put(ORI_MODE_KEY, ORI_MODE_PORTRAIT);
                    break;
                default:
                    break;
            }
            int den = newConfig.densityDpi;
            json.put(DENSITY_KEY, den);
            container.onConfigurationUpdated(json.toString());
        } catch (JSONException ignored) {
            ALog.e(LOG_TAG, "failed parse editing config json");
            return;
        }
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        AceEnv.dump(prefix, fd, writer, args);
    }

    private void createContainer() {
        AceEventCallback callbackHandler = new AceEventCallback() {
            @Override
            public String onEvent(int pageId, String eventId, String param) {
                return onCallbackWithReturn(pageId, eventId, param);
            }

            @Override
            public void onFinish() {
                ALog.i(LOG_TAG, "finish current activity");
                finish();
            }

            @Override
            public void onStatusBarBgColorChanged(int color) {
                statusBarBgColorChanged(color);
            }
        };
        container = AceEnv.createContainer(callbackHandler, activityId, getInstanceName());
        if (container == null) {
            ALog.e(LOG_TAG, "create container failed.");
            return;
        }
        container.setHostClassName(this.getClass().getName());
        initDeviceInfo();
        copyOhosThemeFiles();
        initTheme(container);
        initAsset();
        if (version == VERSION_ETS) {
            // in ets version, we should make sure load page after initialized
            setAceView();
            loadPage();
        } else {
            loadPage();
            setAceView();
        }
    }

    private void loadPage() {
        if (container == null) {
            return;
        }

        // Here we load default page.
        container.loadPageContent("", "");
    }

    private void setAceView() {
        IAceView aceView = container.getView(density, widthPixels, heightPixels);
        if (aceView instanceof View) {
            setContentView((View) aceView);
        }
        aceView.viewCreated();
        aceView.addResourcePlugin(AceVideoPluginAosp.createRegister(this, getInstanceName()));
    }

    private void initAsset() {
        Context context = getApplicationContext();
        container.addAssetPath(getAssets(), ASSET_PATH + getInstanceName());
        container.addAssetPath(getAssets(), ASSET_PATH + ASSET_PATH_SHARE);
        String apkPath = context.getPackageCodePath();
        int lastIndex = apkPath.lastIndexOf("/");
        apkPath = apkPath.substring(0, lastIndex).concat("/lib/arm64");
        container.setLibPath(apkPath);
    }

    /**
     * set the instance name, should called before super.onCreate()
     *
     * @param name the instance name to set
     */
    public void setInstanceName(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        instanceName = name;
    }

    private String getInstanceName() {
        if (instanceName == null) {
            return INSTANCE_DEFAULT_NAME;
        }
        return instanceName;
    }

    /**
     * set app type version
     *
     * @param version the version of app type, can be one of this:
     *                VERSION_JS/VERSION_ETS
     */
    public void setVersion(int version) {
        ALog.i(LOG_TAG, "set app type version:" + version);
        this.version = version;
    }

    /**
     * called when native report callback event
     *
     * @param pageId the id of running page
     * @param callbackId the id of callback event
     * @param jsonStr the params of callback info
     * @return the result of callback
     */
    protected String onCallbackWithReturn(int pageId, String callbackId, String jsonStr) {
        if (callbackId == null || callbackId.isEmpty()) {
            return null;
        }
        ALog.d(LOG_TAG, "onCallbackWithReturn called");
        return "";
    }

    private static int toGray(int red, int green, int blue) {
        // formula fo color to gray
        return (red * 38 + green * 75 + blue * 15) >> 7;
    }

    private void statusBarBgColorChanged(int color) {
        ALog.d(LOG_TAG, "set status bar, light: " + color);
        if (getWindow() == null) {
            return;
        }

        View decorView = getWindow().getDecorView();
        int statusBarVisibility = decorView.getSystemUiVisibility();
        int gray = toGray(Color.red(color), Color.green(color), Color.blue(color));
        boolean isLightColor = gray > GRAY_THRESHOLD;
        if (isLightColor) {
            statusBarVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            statusBarVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(statusBarVisibility);
    }

    private void initDeviceInfo() {
        Resources resource = getResources();
        if (resource != null) {
            boolean isRound = resource.getConfiguration().isScreenRound();
            int orientation = resource.getConfiguration().orientation;
            widthPixels = resource.getDisplayMetrics().widthPixels;
            heightPixels = resource.getDisplayMetrics().heightPixels;
            density = resource.getDisplayMetrics().density;
            int mcc = resource.getConfiguration().mcc;
            int mnc = resource.getConfiguration().mnc;
            container.initDeviceInfo(widthPixels, heightPixels, orientation, density, isRound, mcc, mnc);
        }
    }

    private void initTheme(AceContainer container) {
        int colorMode = AceContainer.COLOR_MODE_LIGHT;
        float fontScale = 1.0f;
        Resources resource = getResources();
        if (resource != null) {
            Configuration configuration = resource.getConfiguration();
            if (configuration != null) {
                int nightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                fontScale = configuration.fontScale;
                colorMode = nightMode == Configuration.UI_MODE_NIGHT_YES ? AceContainer.COLOR_MODE_DARK
                        : AceContainer.COLOR_MODE_LIGHT;
            }
        }
        container.setColorMode(colorMode);
        int themeId = colorMode == AceContainer.COLOR_MODE_LIGHT ? THEME_ID_LIGHT : THEME_ID_DARK;
        ALog.i(LOG_TAG, "init theme, color mode :" + colorMode + " themeId :" + themeId);
        container.initResourceManager(getExternalFilesDir(null).getAbsolutePath(), themeId);
        container.setFontScale(fontScale);
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions permissions The requested permissions. Never null.
     * @param grantResults grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    public synchronized void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Context context = getApplicationContext();
        GrantResult grantResultsClass = new GrantResult(context);
        grantResultsClass.onRequestPremissionCallback(permissions, grantResults);
    }

    private int getUid(Context context) {
        int uid = 0;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                ALog.d(LOG_TAG, "get uid when package manager is null");
                return uid;
            }
            ApplicationInfo applicationInfo = pm.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            uid = applicationInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            ALog.e(LOG_TAG, "get uid failed, error: " + e.getMessage());
        }
        return uid;
    }

    private void copyOhosThemeFiles() {
        copyFilesFromAssets("res/systemres", getExternalFilesDir(null).getAbsolutePath() + "/systemres");
        copyFilesFromAssets("res/appres", getExternalFilesDir(null).getAbsolutePath() + "/appres");
    }

    private void copyFilesFromAssets(String assetsPath, String savePath) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            String[] fileNames = getAssets().list(assetsPath);
            File file = new File(savePath);
            if (fileNames.length > 0) {
                if (!file.exists()) {
                    file.mkdirs();
                }
                for (String fileName : fileNames) {
                    copyFilesFromAssets(assetsPath + "/" + fileName, savePath + "/" + fileName);
                }
            } else {
                if (file.exists()) {
                    return;
                }
                is = getAssets().open(assetsPath);
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
            }
        } catch (IOException e) {
            ALog.e(LOG_TAG, "read or write data err: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    ALog.e(LOG_TAG, "InputStream close err: " + e.getMessage());
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    ALog.e(LOG_TAG, "FileOutputStream close err: " + e.getMessage());
                }
            }
        }
    }
}
