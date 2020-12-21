package kr.co.bootpay;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
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
import kr.co.bootpay.events.TopRenderProcessGoneEvent;
import kr.co.bootpay.events.TopShouldStartLoadWithRequestEvent;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

class BPCWebViewClient extends WebViewClient {
    private static final String TAG = "BPCWebViewClient";
    protected static final int SHOULD_OVERRIDE_URL_LOADING_TIMEOUT = 250;

    protected boolean mLastLoadFailed = false;
    protected @Nullable
    ReadableArray mUrlPrefixesForDefaultIntent;
    protected BPCWebView.ProgressChangedFilter progressChangedFilter = null;
    protected @Nullable String ignoreErrFailedForThisURL = null;

    public void setIgnoreErrFailedForThisURL(@Nullable String url) {
        ignoreErrFailedForThisURL = url;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        super.onPageFinished(webView, url);

        if (!mLastLoadFailed) {
            BPCWebView reactWebView = (BPCWebView) webView;

            reactWebView.callInjectedJavaScript();

            emitFinishEvent(webView, url);
        }
    }

    @Override
    public void onPageStarted(WebView webView, String url, Bitmap favicon) {
        super.onPageStarted(webView, url, favicon);
        mLastLoadFailed = false;

        BPCWebView reactWebView = (BPCWebView) webView;
        reactWebView.callInjectedJavaScriptBeforeContentLoaded();

        BPCWebViewManager.DispatchEvent(
                webView,
                new TopLoadingStartEvent(
                        webView.getId(),
                        createWebViewEvent(webView, url)));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Intent intent = parse(url);
        if (isIntent(url)) {
            if (isExistInfo(intent, view.getContext()) || isExistPackage(intent, view.getContext()))
                return start(intent, view.getContext());
            else
                return gotoMarket(intent, view.getContext());
        } else if (isMarket(url)) {
            if (!(isExistInfo(intent, view.getContext()) || isExistPackage(intent, view.getContext())))
                return gotoMarket(intent, view.getContext());
            else
//          return true;
                return shouldOverrideUrlLoadingRN(view, url);
        } else if (isSpecialCase(url)) {
            if (isExistInfo(intent, view.getContext()) || isExistPackage(intent, view.getContext()))
                return start(intent, view.getContext());
            else
                return gotoMarket(intent, view.getContext());
        }
//      return url.contains("vguardend");
        return shouldOverrideUrlLoadingRN(view, url);
    }


    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final String url = request.getUrl().toString();
        return this.shouldOverrideUrlLoading(view, url);
    }


    private boolean shouldOverrideUrlLoadingRN(WebView view, String url) {
        final BPCWebView BPCWebView = (BPCWebView) view;
        final boolean isJsDebugging = ((ReactContext) view.getContext()).getJavaScriptContextHolder().get() == 0;

        if (!isJsDebugging && BPCWebView.mCatalystInstance != null) {
            final Pair<Integer, AtomicReference<BPCWebViewModule.ShouldOverrideUrlLoadingLock.ShouldOverrideCallbackState>> lock = BPCWebViewModule.shouldOverrideUrlLoadingLock.getNewLock();
            final int lockIdentifier = lock.first;
            final AtomicReference<BPCWebViewModule.ShouldOverrideUrlLoadingLock.ShouldOverrideCallbackState> lockObject = lock.second;

            final WritableMap event = createWebViewEvent(view, url);
            event.putInt("lockIdentifier", lockIdentifier);
            BPCWebView.sendDirectMessage("onShouldStartLoadWithRequest", event);

            try {
                assert lockObject != null;
                synchronized (lockObject) {
                    final long startTime = SystemClock.elapsedRealtime();
                    while (lockObject.get() == BPCWebViewModule.ShouldOverrideUrlLoadingLock.ShouldOverrideCallbackState.UNDECIDED) {
                        if (SystemClock.elapsedRealtime() - startTime > SHOULD_OVERRIDE_URL_LOADING_TIMEOUT) {
                            FLog.w(TAG, "Did not receive response to shouldOverrideUrlLoading in time, defaulting to allow loading.");
                            BPCWebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);
                            return false;
                        }
                        lockObject.wait(SHOULD_OVERRIDE_URL_LOADING_TIMEOUT);
                    }
                }
            } catch (InterruptedException e) {
                FLog.e(TAG, "shouldOverrideUrlLoading was interrupted while waiting for result.", e);
                BPCWebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);
                return false;
            }

            final boolean shouldOverride = lockObject.get() == BPCWebViewModule.ShouldOverrideUrlLoadingLock.ShouldOverrideCallbackState.SHOULD_OVERRIDE;
            BPCWebViewModule.shouldOverrideUrlLoadingLock.removeLock(lockIdentifier);

            return shouldOverride;
        } else {
            FLog.w(TAG, "Couldn't use blocking synchronous call for onShouldStartLoadWithRequest due to debugging or missing Catalyst instance, falling back to old event-and-load.");
            progressChangedFilter.setWaitingForCommandLoadUrl(true);
            BPCWebViewManager.DispatchEvent(
                    view,
                    new TopShouldStartLoadWithRequestEvent(
                            view.getId(),
                            createWebViewEvent(view, url)));
            return true;
        }
    }


    private Boolean isSpecialCase(String url) {
        return url.matches("^shinhan\\S+$")
                || url.startsWith("kftc-bankpay://")
                || url.startsWith("v3mobileplusweb://")
                || url.startsWith("hdcardappcardansimclick://")
                || url.startsWith("nidlogin://")
                || url.startsWith("mpocket.online.ansimclick://");
    }

    private Intent parse(String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if(intent.getPackage() == null) {
                if (url == null) return intent;
                if (url.startsWith("shinhan-sr-ansimclick")) intent.setPackage("com.shcard.smartpay");
                else if (url.startsWith("kftc-bankpay")) intent.setPackage("com.kftc.bankpay.android");
                else if (url.startsWith("ispmobile")) intent.setPackage("kvp.jjy.MispAndroid320");
                else if (url.startsWith("hdcardappcardansimclick")) intent.setPackage("com.hyundaicard.appcard");
                else if (url.startsWith("kb-acp")) intent.setPackage("com.kbcard.kbkookmincard");
                else if (url.startsWith("mpocket.online.ansimclick")) intent.setPackage("kr.co.samsungcard.mpocket");
                else if (url.startsWith("lotteappcard")) intent.setPackage("com.lcacApp");
                else if (url.startsWith("cloudpay")) intent.setPackage("com.hanaskcard.paycla");
                else if (url.startsWith("nhappvardansimclick")) intent.setPackage("nh.smart.nhallonepay");
                else if (url.startsWith("citispay")) intent.setPackage("kr.co.citibank.citimobile");
                else if (url.startsWith("kakaotalk")) intent.setPackage("com.kakao.talk");
            }
            return intent;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Boolean isIntent(String url) {
//                return url.matches("^intent:?\\w*://\\S+$");
        return url.startsWith("intent:");
    }

    private Boolean isMarket(String url) {
        return url.matches("^market://\\S+$");
    }



    private Boolean isExistInfo(Intent intent, Context context) {
        try {
            return intent != null && context.getPackageManager().getPackageInfo(intent.getPackage(), PackageManager.GET_ACTIVITIES) != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Boolean isExistPackage(Intent intent, Context context) {
        return intent != null &&  intent.getPackage() != null && context.getPackageManager().getLaunchIntentForPackage(intent.getPackage()) != null;
    }

    private boolean start(Intent intent, Context context) {
        context.startActivity(intent);
        return true;
    }

    private boolean gotoMarket(Intent intent, Context context) {
        final String appPackageName = intent.getPackage();
        if(appPackageName == null) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, intent.getData()));
            return true;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
        return true;
    }

//    @Override
//    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
//      if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) back();
//      return super.shouldOverrideKeyEvent(view, event);
//    }


//    boolean back() {
//      if (canGoBack()) goBack();
//      else if(context != null) dialog.dismiss();
//      return true;
//    }

    @Override
    public void onReceivedSslError(final WebView webView, final SslErrorHandler handler, final SslError error) {
        // onReceivedSslError is called for most requests, per Android docs: https://developer.android.com/reference/android/webkit/WebViewClient#onReceivedSslError(android.webkit.WebView,%2520android.webkit.SslErrorHandler,%2520android.net.http.SslError)
        // WebView.getUrl() will return the top-level window URL.
        // If a top-level navigation triggers this error handler, the top-level URL will be the failing URL (not the URL of the currently-rendered page).
        // This is desired behavior. We later use these values to determine whether the request is a top-level navigation or a subresource request.
        String topWindowUrl = webView.getUrl();
        String failingUrl = error.getUrl();

        // Cancel request after obtaining top-level URL.
        // If request is cancelled before obtaining top-level URL, undesired behavior may occur.
        // Undesired behavior: Return value of WebView.getUrl() may be the current URL instead of the failing URL.
        handler.cancel();

        if (!topWindowUrl.equalsIgnoreCase(failingUrl)) {
            // If error is not due to top-level navigation, then do not call onReceivedError()
            Log.w("BPCWebViewManager", "Resource blocked from loading due to SSL error. Blocked URL: "+failingUrl);
            return;
        }

        int code = error.getPrimaryError();
        String description = "";
        String descriptionPrefix = "SSL error: ";

        // https://developer.android.com/reference/android/net/http/SslError.html
        switch (code) {
            case SslError.SSL_DATE_INVALID:
                description = "The date of the certificate is invalid";
                break;
            case SslError.SSL_EXPIRED:
                description = "The certificate has expired";
                break;
            case SslError.SSL_IDMISMATCH:
                description = "Hostname mismatch";
                break;
            case SslError.SSL_INVALID:
                description = "A generic error occurred";
                break;
            case SslError.SSL_NOTYETVALID:
                description = "The certificate is not yet valid";
                break;
            case SslError.SSL_UNTRUSTED:
                description = "The certificate authority is not trusted";
                break;
            default:
                description = "Unknown SSL Error";
                break;
        }

        description = descriptionPrefix + description;

        this.onReceivedError(
                webView,
                code,
                description,
                failingUrl
        );
    }

    @Override
    public void onReceivedError(
            WebView webView,
            int errorCode,
            String description,
            String failingUrl) {

        if (ignoreErrFailedForThisURL != null
                && failingUrl.equals(ignoreErrFailedForThisURL)
                && errorCode == -1
                && description.equals("net::ERR_FAILED")) {

            // This is a workaround for a bug in the WebView.
            // See these chromium issues for more context:
            // https://bugs.chromium.org/p/chromium/issues/detail?id=1023678
            // https://bugs.chromium.org/p/chromium/issues/detail?id=1050635
            // This entire commit should be reverted once this bug is resolved in chromium.
            setIgnoreErrFailedForThisURL(null);
            return;
        }

        super.onReceivedError(webView, errorCode, description, failingUrl);
        mLastLoadFailed = true;

        // In case of an error JS side expect to get a finish event first, and then get an error event
        // Android WebView does it in the opposite way, so we need to simulate that behavior
        emitFinishEvent(webView, failingUrl);

        WritableMap eventData = createWebViewEvent(webView, failingUrl);
        eventData.putDouble("code", errorCode);
        eventData.putString("description", description);

        BPCWebViewManager.DispatchEvent(
                webView,
                new TopLoadingErrorEvent(webView.getId(), eventData));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedHttpError(
            WebView webView,
            WebResourceRequest request,
            WebResourceResponse errorResponse) {
        super.onReceivedHttpError(webView, request, errorResponse);

        if (request.isForMainFrame()) {
            WritableMap eventData = createWebViewEvent(webView, request.getUrl().toString());
            eventData.putInt("statusCode", errorResponse.getStatusCode());
            eventData.putString("description", errorResponse.getReasonPhrase());

            BPCWebViewManager.DispatchEvent(
                    webView,
                    new TopHttpErrorEvent(webView.getId(), eventData));
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onRenderProcessGone(WebView webView, RenderProcessGoneDetail detail) {
        // WebViewClient.onRenderProcessGone was added in O.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        super.onRenderProcessGone(webView, detail);

        if(detail.didCrash()){
            Log.e("BPCWebViewManager", "The WebView rendering process crashed.");
        }
        else{
            Log.w("BPCWebViewManager", "The WebView rendering process was killed by the system.");
        }

        // if webView is null, we cannot return any event
        // since the view is already dead/disposed
        // still prevent the app crash by returning true.
        if(webView == null){
            return true;
        }

        WritableMap event = createWebViewEvent(webView, webView.getUrl());
        event.putBoolean("didCrash", detail.didCrash());

        BPCWebViewManager.DispatchEvent(
                webView,
                new TopRenderProcessGoneEvent(webView.getId(), event)
        );

        // returning false would crash the app.
        return true;
    }

    protected void emitFinishEvent(WebView webView, String url) {
        BPCWebViewManager.DispatchEvent(
                webView,
                new TopLoadingFinishEvent(
                        webView.getId(),
                        createWebViewEvent(webView, url)));
    }

    protected WritableMap createWebViewEvent(WebView webView, String url) {
        WritableMap event = Arguments.createMap();
        event.putDouble("target", webView.getId());
        // Don't use webView.getUrl() here, the URL isn't updated to the new value yet in callbacks
        // like onPageFinished
        event.putString("url", url);
        event.putBoolean("loading", !mLastLoadFailed && webView.getProgress() != 100);
        event.putString("title", webView.getTitle());
        event.putBoolean("canGoBack", webView.canGoBack());
        event.putBoolean("canGoForward", webView.canGoForward());
        return event;
    }

    public void setUrlPrefixesForDefaultIntent(ReadableArray specialUrls) {
        mUrlPrefixesForDefaultIntent = specialUrls;
    }

    public void setProgressChangedFilter(BPCWebView.ProgressChangedFilter filter) {
        progressChangedFilter = filter;
    }
}
