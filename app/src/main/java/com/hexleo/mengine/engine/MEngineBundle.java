package com.hexleo.mengine.engine;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.hexleo.mengine.activity.BaseActivity;
import com.hexleo.mengine.application.BaseApplication;
import com.hexleo.mengine.engine.bridge.MeJsBridge;
import com.hexleo.mengine.engine.config.MeBundleConfig;
import com.hexleo.mengine.engine.jscore.MeJsContextFactory;
import com.hexleo.mengine.engine.jscore.MeJsContext;
import com.hexleo.mengine.engine.webview.MeWebView;
import com.hexleo.mengine.engine.webview.MeWebViewFactory;
import com.hexleo.mengine.util.FileHelper;
import com.hexleo.mengine.util.MLog;
import com.hexleo.mengine.util.ThreadManager;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by hexleo on 2017/2/7.
 */

public class MEngineBundle {

    private static final String TAG = "MEngineBundle";

    // 整个App共享全局变量
    private static Map<String, String> sGlobalVar = new Hashtable<>();

    private String mBundleName;
    private MeBundleConfig mConfig;
    private String mJsFileCache;
    private String mIndexHtmlPath;
    private boolean isInit; // bundle是否已经初始化

    private MeJsBridge mJsBridge;
    private MeJsContext mJsContext;
    private MeWebView mWebView;
    private WeakReference<BaseActivity> mActivityRef;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private MEngineBundle(MeBundleConfig config) {
        isInit = false;
        mConfig = config;
        mBundleName = config.bundleName;
        mActivityRef = new WeakReference<>(null);
        mIndexHtmlPath = FileHelper.getIndexPath(mBundleName);
        if (!mConfig.lazyInit) {
            initRuntime(null);
        }
    }

    public static MEngineBundle newInstance(MeBundleConfig config) {
        return new MEngineBundle(config);
    }

    public String getBundleName() {
        return mBundleName;
    }

    public MeBundleConfig getBundleConfig() {
        return mConfig;
    }

    public static void putGlobalVar(String key, String value) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            sGlobalVar.put(key, value);
        }
    }

    public static String getGlobalVar(String key) {
        String value = sGlobalVar.get(key);
        return value == null? "" : value;
    }

    public void setActivity(BaseActivity activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    public BaseActivity getActivity() {
        return mActivityRef.get();
    }

    private void initRuntime(MeWebView.MeWebViewListener listener) {
        initJsBridge();
        initJsContext();
        // JsContext必须要在WebView初始化前完成初始化
        initWebView(listener);
        isInit = true;
    }

    private void initJsBridge() {
        // 创建JsBridge 加载公共类
        mJsBridge = new MeJsBridge(this);
    }

    private void initJsContext() {
        // 加载js文件
        if (TextUtils.isEmpty(mJsFileCache)) {
            mJsFileCache = FileHelper.getAppJs(mBundleName, BaseApplication.getBaseApplication());
        }
        // 创建JsContext
        mJsContext = MeJsContextFactory.getInstance().create();
        // JsBridge加载JsContext
        mJsBridge.initJsContext(mJsContext);
        // 最后加载文件
        mJsContext.runScript(mJsFileCache);
    }

    private void initWebView(final MeWebView.MeWebViewListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView = MeWebViewFactory.getInstance().create(BaseApplication.getBaseApplication(), mJsBridge);
                // 加载index.html文件  不使用loadData是防止部分机型出现乱码问题
                mWebView.loadUrl(mIndexHtmlPath);
                // 文件加载完成后在加载js组件
                mJsBridge.initWebView(mWebView);
                if (listener != null) {
                    listener.OnWebViewReady(mWebView);
                }
            }
        });
    }

    public void getWebView(final MeWebView.MeWebViewListener listener) {
        if (listener == null) {
            return;
        }
        if (mConfig.lazyInit && !isInit) {
            MLog.d(TAG, "getWebView webview lazy init");
            ThreadManager.post(new Runnable() {
                @Override
                public void run() {
                    initRuntime(listener);
                }
            });
        } else {
            MLog.d(TAG, "getWebView webview is ready");
            listener.OnWebViewReady(mWebView);
        }
    }

    public MeJsBridge getJsBridge() {
        return mJsBridge;
    }

    /**
     * 销毁时回调
     */
    public void destory() {
    }

}