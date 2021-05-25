package kr.co.bootpay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import kr.co.bootpay.events.TopHttpErrorEvent;
import kr.co.bootpay.events.TopLoadingErrorEvent;
import kr.co.bootpay.events.TopLoadingFinishEvent;
import kr.co.bootpay.events.TopLoadingStartEvent;
import kr.co.bootpay.events.TopMessageEvent;
import kr.co.bootpay.events.TopRenderProcessGoneEvent;
import kr.co.bootpay.events.TopShouldStartLoadWithRequestEvent;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.ScrollEvent;
import com.facebook.react.views.scroll.ScrollEventType;
import java.util.ArrayList;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Subclass of {@link WebView} that implements {@link LifecycleEventListener} interface in order
 * to call {@link WebView#destroy} on activity destroy event and also to clear the client
 */
class BPCWebView extends WebView implements LifecycleEventListener {
    protected @Nullable
    String injectedJS;
    protected @Nullable
    ArrayList<String> injectedJSBeforeContentLoaded;

    /**
     * android.webkit.WebChromeClient fundamentally does not support JS injection into frames other
     * than the main frame, so these two properties are mostly here just for parity with iOS & macOS.
     */
    protected boolean injectedJavaScriptForMainFrameOnly = true;
    protected boolean injectedJavaScriptBeforeContentLoadedForMainFrameOnly = true;

    protected boolean messagingEnabled = false;
    protected @Nullable
    String messagingModuleName;
    protected @Nullable
    BPCWebViewClient mBPCWebViewClient;
    protected @Nullable
    CatalystInstance mCatalystInstance;
    protected boolean sendContentSizeChangeEvents = false;
    private OnScrollDispatchHelper mOnScrollDispatchHelper;
    protected boolean hasScrollEvent = false;
    protected ProgressChangedFilter progressChangedFilter;

    protected static final String JAVASCRIPT_INTERFACE = "ReactNativeWebView";

    /**
     * WebView must be created with an context of the current activity
     * <p>
     * Activity Context is required for creation of dialogs internally by WebView
     * Reactive Native needed for access to ReactNative internal system functionality
     */
    public BPCWebView(ThemedReactContext reactContext) {
        super(reactContext);
        this.injectedJSBeforeContentLoaded = new ArrayList();
        this.createCatalystInstance();
        progressChangedFilter = new ProgressChangedFilter();
    }

    public void setIgnoreErrFailedForThisURL(String url) {
        mBPCWebViewClient.setIgnoreErrFailedForThisURL(url);
    }

    public void setSendContentSizeChangeEvents(boolean sendContentSizeChangeEvents) {
        this.sendContentSizeChangeEvents = sendContentSizeChangeEvents;
    }

    public void setHasScrollEvent(boolean hasScrollEvent) {
        this.hasScrollEvent = hasScrollEvent;
    }

    @Override
    public void onHostResume() {
        // do nothing
    }

    @Override
    public void onHostPause() {
        // do nothing
    }

    @Override
    public void onHostDestroy() {
        cleanupCallbacksAndDestroy();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);

        if (sendContentSizeChangeEvents) {
            BPCWebViewManager.DispatchEvent(
                    this,
                    new ContentSizeChangeEvent(
                            this.getId(),
                            w,
                            h
                    )
            );
        }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(client);
        if (client instanceof BPCWebViewClient) {
            mBPCWebViewClient = (BPCWebViewClient) client;
            mBPCWebViewClient.setProgressChangedFilter(progressChangedFilter);
        }
    }

    WebChromeClient mWebChromeClient;
    @Override
    public void setWebChromeClient(WebChromeClient client) {
        this.mWebChromeClient = client;
        super.setWebChromeClient(client);
        if (client instanceof BPCWebChromeClient) {
            ((BPCWebChromeClient) client).setProgressChangedFilter(progressChangedFilter);
        }
    }

    public @Nullable
    BPCWebViewClient getBPCWebViewClient() {
        return mBPCWebViewClient;
    }

    public void setInjectedJavaScript(@Nullable String js) {
      injectedJS = js;
    }

    // callJavaScript
    public void callJavaScript(@Nullable String js) {
         if (getSettings().getJavaScriptEnabled() &&
                js != null &&
                !TextUtils.isEmpty(js)) {
            evaluateJavascriptWithFallback("(function() {\n" + js + ";\n})();");
        }
    }

    public void setInjectedJavaScriptBeforeContentLoaded(ArrayList<String> jsList) {
        injectedJSBeforeContentLoaded = jsList;
    }

    public void appendJavaScriptBeforeContentLoaded(String js) {
        injectedJSBeforeContentLoaded.add(js);
    }

    public void setInjectedJavaScriptForMainFrameOnly(boolean enabled) {
        injectedJavaScriptForMainFrameOnly = enabled;
    }

    public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(boolean enabled) {
        injectedJavaScriptBeforeContentLoadedForMainFrameOnly = enabled;
    }

    protected BPCWebViewBridge createBPCWebViewBridge(BPCWebView webView) {
        return new BPCWebViewBridge(webView);
    }

    protected void createCatalystInstance() {
        ReactContext reactContext = (ReactContext) this.getContext();

        if (reactContext != null) {
            mCatalystInstance = reactContext.getCatalystInstance();
        }
    }

    public void startBootpay() {
      callInjectedJavaScriptBeforeContentLoaded();
        callInjectedJavaScript();
    }

    @SuppressLint("AddJavascriptInterface")
    public void setMessagingEnabled(boolean enabled) {
        if (messagingEnabled == enabled) {
            return;
        }

        messagingEnabled = enabled;

        if (enabled) {
            addJavascriptInterface(createBPCWebViewBridge(this), JAVASCRIPT_INTERFACE);
        } else {
            removeJavascriptInterface(JAVASCRIPT_INTERFACE);
        }
    }

    public void setMessagingModuleName(String moduleName) {
        messagingModuleName = moduleName;
    }

    protected void evaluateJavascriptWithFallback(String script) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(script, null);
            return;
        }

        try {
            loadUrl("javascript:" + URLEncoder.encode(script, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should always be supported
            throw new RuntimeException(e);
        }
    }

    public void callInjectedJavaScript() {
        if (getSettings().getJavaScriptEnabled() &&
                injectedJS != null &&
                !TextUtils.isEmpty(injectedJS)) {
            evaluateJavascriptWithFallback("(function() {\n" + injectedJS + ";\n})();");
        }
    }

    public void callInjectedJavaScriptBeforeContentLoaded() {
        if (getSettings().getJavaScriptEnabled() && injectedJSBeforeContentLoaded != null) {
          for(String js : injectedJSBeforeContentLoaded) {
            evaluateJavascriptWithFallback("(function() {\n" + js + ";\n})();");
          }
        }
    }

    public void onMessage(String message) {
        ReactContext reactContext = (ReactContext) this.getContext();
        BPCWebView mContext = this;

        if (mBPCWebViewClient != null) {
            WebView webView = this;
            webView.post(new Runnable() {
                @Override
                public void run() {
                    if (mBPCWebViewClient == null) {
                        return;
                    }
                    WritableMap data = mBPCWebViewClient.createWebViewEvent(webView, webView.getUrl());
                    data.putString("data", message);

                    if (mCatalystInstance != null) {
                        mContext.sendDirectMessage("onMessage", data);
                    } else {
                        BPCWebViewManager.DispatchEvent(webView, new TopMessageEvent(webView.getId(), data));
                    }
                }
            });
        } else {
            WritableMap eventData = Arguments.createMap();
            eventData.putString("data", message);

            if (mCatalystInstance != null) {
                this.sendDirectMessage("onMessage", eventData);
            } else {
                BPCWebViewManager.DispatchEvent(this, new TopMessageEvent(this.getId(), eventData));
            }
        }
    }

    protected void sendDirectMessage(final String method, WritableMap data) {
        WritableNativeMap event = new WritableNativeMap();
        event.putMap("nativeEvent", data);

        WritableNativeArray params = new WritableNativeArray();
        params.pushMap(event);

        mCatalystInstance.callFunction(messagingModuleName, method, params);
    }

    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);

        if (!hasScrollEvent) {
            return;
        }

        if (mOnScrollDispatchHelper == null) {
            mOnScrollDispatchHelper = new OnScrollDispatchHelper();
        }

        if (mOnScrollDispatchHelper.onScrollChanged(x, y)) {
            ScrollEvent event = ScrollEvent.obtain(
                    this.getId(),
                    ScrollEventType.SCROLL,
                    x,
                    y,
                    mOnScrollDispatchHelper.getXFlingVelocity(),
                    mOnScrollDispatchHelper.getYFlingVelocity(),
                    this.computeHorizontalScrollRange(),
                    this.computeVerticalScrollRange(),
                    this.getWidth(),
                    this.getHeight());

            BPCWebViewManager.DispatchEvent(this, event);
        }
    }

    protected void cleanupCallbacksAndDestroy() {
        setWebViewClient(null);
        destroy();
    }

    @Override
    public void destroy() {
        if (mWebChromeClient != null) {
            mWebChromeClient.onHideCustomView();
        }
        super.destroy();
    }

    protected class BPCWebViewBridge {
        BPCWebView mContext;

        BPCWebViewBridge(BPCWebView c) {
            mContext = c;
        }

        /**
         * This method is called whenever JavaScript running within the web view calls:
         * - window[JAVASCRIPT_INTERFACE].postMessage
         */
        @JavascriptInterface
        public void postMessage(String message) {
            mContext.onMessage(message);
        }
    }

    protected static class ProgressChangedFilter {
        private boolean waitingForCommandLoadUrl = false;

        public void setWaitingForCommandLoadUrl(boolean isWaiting) {
            waitingForCommandLoadUrl = isWaiting;
        }

        public boolean isWaitingForCommandLoadUrl() {
            return waitingForCommandLoadUrl;
        }
    }

}
