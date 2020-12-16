package kr.co.bootpay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.bootpaytemp.events.TopHttpErrorEvent;
import com.bootpaytemp.events.TopLoadingErrorEvent;
import com.bootpaytemp.events.TopLoadingFinishEvent;
import com.bootpaytemp.events.TopLoadingProgressEvent;
import com.bootpaytemp.events.TopLoadingStartEvent;
import com.bootpaytemp.events.TopMessageEvent;
import com.bootpaytemp.events.TopRenderProcessGoneEvent;
import com.bootpaytemp.events.TopShouldStartLoadWithRequestEvent;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.ScrollEvent;
import com.facebook.react.views.scroll.ScrollEventType;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

class BPCWebChromeClient extends WebChromeClient implements LifecycleEventListener {


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected static final int FULLSCREEN_SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    protected ReactContext mReactContext;
    protected View mWebView;
    protected View mVideoView;
    protected CustomViewCallback mCustomViewCallback;

    protected BPCWebView.ProgressChangedFilter progressChangedFilter = null;

    public BPCWebChromeClient(ReactContext reactContext, WebView webView) {
        this.mReactContext = reactContext;
        this.mWebView = webView;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

//        final BPCWebView newWebView = new BPCWebView((ThemedReactContext) view.getContext());
        final BPCWebView newWebView = BPCWebViewManager.CreateViewInstance((ThemedReactContext) view.getContext(), true);
        BPCWebViewManager.SetupWebChromeClient((ReactContext) view.getContext(), newWebView);
        BPCReactProp.settingPropAll(newWebView);

//        WebSettings webSettings = newWebView.getSettings();
//        webSettings.setJavaScriptEnabled(true);

        final Dialog dialog = new Dialog(view.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(newWebView);

        ViewGroup.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        dialog.show();

        newWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView window) {
                dialog.dismiss();
            }
        });
//
//
//        // WebView Popup에서 내용이 안보이고 빈 화면만 보여 아래 코드 추가
//        newWebView.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                return false;
//            }
//        });

        ((WebView.WebViewTransport)resultMsg.obj).setWebView(newWebView);
        resultMsg.sendToTarget();
        return true;
    }

    @Override
    public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
        getRootView().removeView(window);
        window.setVisibility(View.GONE);
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                .setMessage(message)
                .setCancelable(true)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        result.confirm();
        return true;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage message) {
        if (ReactBuildConfig.DEBUG) {
            return super.onConsoleMessage(message);
        }
        // Ignore console logs in non debug builds.
        return true;
    }

    // Fix WebRTC permission request error.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        String[] requestedResources = request.getResources();
        ArrayList<String> permissions = new ArrayList<>();
        ArrayList<String> grantedPermissions = new ArrayList<String>();
        for (int i = 0; i < requestedResources.length; i++) {
            if (requestedResources[i].equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            } else if (requestedResources[i].equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            // TODO: RESOURCE_MIDI_SYSEX, RESOURCE_PROTECTED_MEDIA_ID.
        }

        for (int i = 0; i < permissions.size(); i++) {
            if (ContextCompat.checkSelfPermission(mReactContext, permissions.get(i)) != PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            if (permissions.get(i).equals(Manifest.permission.RECORD_AUDIO)) {
                grantedPermissions.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
            } else if (permissions.get(i).equals(Manifest.permission.CAMERA)) {
                grantedPermissions.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
            }
        }

        if (grantedPermissions.isEmpty()) {
            request.deny();
        } else {
            String[] grantedPermissionsArray = new String[grantedPermissions.size()];
            grantedPermissionsArray = grantedPermissions.toArray(grantedPermissionsArray);
            request.grant(grantedPermissionsArray);
        }
    }

    @Override
    public void onProgressChanged(WebView webView, int newProgress) {
        super.onProgressChanged(webView, newProgress);
        final String url = webView.getUrl();
        if (progressChangedFilter.isWaitingForCommandLoadUrl()) {
            return;
        }
        WritableMap event = Arguments.createMap();
        event.putDouble("target", webView.getId());
        event.putString("title", webView.getTitle());
        event.putString("url", url);
        event.putBoolean("canGoBack", webView.canGoBack());
        event.putBoolean("canGoForward", webView.canGoForward());
        event.putDouble("progress", (float) newProgress / 100);
        BPCWebViewManager.DispatchEvent(
                webView,
                new TopLoadingProgressEvent(
                        webView.getId(),
                        event));
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType) {
        BPCWebViewManager.GetModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback) {
        BPCWebViewManager.GetModule(mReactContext).startPhotoPickerIntent(filePathCallback, "");
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType, String capture) {
        BPCWebViewManager.GetModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
        return BPCWebViewManager.GetModule(mReactContext).startPhotoPickerIntent(filePathCallback, acceptTypes, allowMultiple);
    }

    @Override
    public void onHostResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mVideoView != null && mVideoView.getSystemUiVisibility() != FULLSCREEN_SYSTEM_UI_VISIBILITY) {
            mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
        }
    }

    @Override
    public void onHostPause() { }

    @Override
    public void onHostDestroy() { }

    protected ViewGroup getRootView() {
        return (ViewGroup) mReactContext.getCurrentActivity().findViewById(android.R.id.content);
    }

    public void setProgressChangedFilter(BPCWebView.ProgressChangedFilter filter) {
        progressChangedFilter = filter;
    }
}
