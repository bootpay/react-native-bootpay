package kr.co.bootpay;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;


import com.facebook.react.views.scroll.ScrollEventType;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;

import kr.co.bootpay.events.TopHttpErrorEvent;
import kr.co.bootpay.events.TopLoadingProgressEvent;
import kr.co.bootpay.events.TopShouldStartLoadWithRequestEvent;
import kr.co.bootpay.events.TopRenderProcessGoneEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages instances of {@link WebView}
 * <p>
 * Can accept following commands:
 * - GO_BACK
 * - GO_FORWARD
 * - RELOAD
 * - LOAD_URL
 * <p>
 * {@link WebView} instances could emit following direct events:
 * - topLoadingFinish
 * - topLoadingStart
 * - topLoadingStart
 * - topLoadingProgress
 * - topShouldStartLoadWithRequest
 * <p>
 * Each event will carry the following properties:
 * - target - view's react tag
 * - url - url set for the webview
 * - loading - whether webview is in a loading state
 * - title - title of the current page
 * - canGoBack - boolean, whether there is anything on a history stack to go back
 * - canGoForward - boolean, whether it is possible to request GO_FORWARD command
 */
@ReactModule(name = BPCWebViewManager.REACT_CLASS)
public class BPCWebViewManager extends SimpleViewManager<WebView> {
  protected static final FrameLayout.LayoutParams FULLSCREEN_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
  private static final String TAG = "BPCWebViewManager";

  public static final int COMMAND_GO_BACK = 1;
  public static final int COMMAND_GO_FORWARD = 2;
  public static final int COMMAND_RELOAD = 3;
  public static final int COMMAND_STOP_LOADING = 4;
  public static final int COMMAND_POST_MESSAGE = 5;
  public static final int COMMAND_INJECT_JAVASCRIPT = 6; //call evaluate javascript
  public static final int COMMAND_LOAD_URL = 7;
  public static final int COMMAND_FOCUS = 8;
  public static final int COMMAND_APPEND_BEFORE_JAVASCRIPT = 9;
  public static final int COMMAND_CALL_JAVASCRIPT = 10;
  public static final int COMMAND_START_BOOTPAY = 11;

  // android commands
  public static final int COMMAND_CLEAR_FORM_DATA = 1000;
  public static final int COMMAND_CLEAR_CACHE = 1001;
  public static final int COMMAND_CLEAR_HISTORY = 1002;

  protected static final String REACT_CLASS = "BPCWebView";

  protected WebViewConfig mWebViewConfig;

  protected BPCWebChromeClient mWebChromeClient = null;
  protected boolean mAllowsFullscreenVideo = false;
  protected @Nullable String mUserAgent = null;
  protected @Nullable String mUserAgentWithApplicationName = null;
  protected ThemedReactContext mThemedReactContext;

  public BPCWebViewManager() {
    mWebViewConfig = new WebViewConfig() {
      public void configWebView(WebView webView) {
      }
    };
  }

  public BPCWebViewManager(WebViewConfig webViewConfig) {
    mWebViewConfig = webViewConfig;
  }

  public static void DispatchEvent(WebView webView, Event event) {
    ReactContext reactContext = (ReactContext) webView.getContext();
    EventDispatcher eventDispatcher =
      reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
    eventDispatcher.dispatchEvent(event);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

//  protected BPCWebView createBPCWebViewInstance(ThemedReactContext reactContext) {
//    this.mThemedReactContext = reactContext;
//    return new BPCWebView(reactContext);
//  }

  @Override
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public WebView createViewInstance(ThemedReactContext reactContext) {
    this.mThemedReactContext = reactContext;
    return BPCWebViewManager.CreateViewInstance(reactContext, false);
  }

  static public BPCWebView CreateViewInstance(ThemedReactContext reactContext, boolean setViewClient) {
    BPCWebView webView = new BPCWebView(reactContext);
    SetupWebChromeClient(reactContext, webView);
    reactContext.addLifecycleEventListener(webView);
    if(setViewClient) webView.setWebViewClient(new BPCWebViewClient());
//    mWebViewConfig.configWebView(webView);
    WebSettings settings = webView.getSettings();

    settings.setAppCacheEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    settings.setDomStorageEnabled(true);
    settings.setJavaScriptEnabled(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setLoadsImagesAutomatically(true);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    settings.setSupportMultipleWindows(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      settings.setAllowFileAccessFromFileURLs(false);
      BPCReactProp.settingProp("allowUniversalAccessFromFileURLs", false, (BPCWebView) webView);
//      BPCReactProp.getInstance().map.put("allowUniversalAccessFromFileURLs", false);
    }
    BPCReactProp.settingProp("mixedContentMode", "always", (BPCWebView) webView);
//    BPCReactProp.getInstance().map.put("mixedContentMode", "always");

    // Fixes broken full-screen modals/galleries due to body height being 0.
    webView.setLayoutParams(
            new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

    if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      WebView.setWebContentsDebuggingEnabled(true);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
          CookieManager.getInstance().setAcceptCookie(true);
          CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
    }

    webView.setDownloadListener(new DownloadListener() {
      public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        webView.setIgnoreErrFailedForThisURL(url);

        BPCWebViewModule module = GetModule(reactContext);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        String downloadMessage = "Downloading " + fileName;

        //Attempt to add cookie, if it exists
        URL urlObj = null;
        try {
          urlObj = new URL(url);
          String baseUrl = urlObj.getProtocol() + "://" + urlObj.getHost();
          String cookie = CookieManager.getInstance().getCookie(baseUrl);
          request.addRequestHeader("Cookie", cookie);
        } catch (MalformedURLException e) {
          System.out.println("Error getting cookie for DownloadManager: " + e.toString());
          e.printStackTrace();
        }

        //Finish setting up request
        request.addRequestHeader("User-Agent", userAgent);
        request.setTitle(fileName);
        request.setDescription(downloadMessage);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        module.setDownloadRequest(request);

        if (module.grantFileDownloaderPermissions()) {
          module.downloadFile();
        }
      }
    });


    return webView;
  }

  @ReactProp(name = "javaScriptEnabled")
  public void setJavaScriptEnabled(WebView view, boolean enabled) {
    BPCReactProp.settingProp("javaScriptEnabled", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("javaScriptEnabled", enabled);
//    view.getSettings().setJavaScriptEnabled(enabled);
  }

  @ReactProp(name = "setSupportMultipleWindows")
  public void setSupportMultipleWindows(WebView view, boolean enabled){
    BPCReactProp.settingProp("setSupportMultipleWindows", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("setSupportMultipleWindows", enabled);
//    view.getSettings().setSupportMultipleWindows(enabled);
  }

  @ReactProp(name = "showsHorizontalScrollIndicator")
  public void setShowsHorizontalScrollIndicator(WebView view, boolean enabled) {
    BPCReactProp.settingProp("showsHorizontalScrollIndicator", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("showsHorizontalScrollIndicator", enabled);
//    view.setHorizontalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "showsVerticalScrollIndicator")
  public void setShowsVerticalScrollIndicator(WebView view, boolean enabled) {
    BPCReactProp.settingProp("showsVerticalScrollIndicator", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("showsVerticalScrollIndicator", enabled);
//    view.setVerticalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "cacheEnabled")
  public void setCacheEnabled(WebView view, boolean enabled) {
    BPCReactProp.settingProp("cacheEnabled", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("cacheEnabled", enabled);
//    if (enabled) {
//      Context ctx = view.getContext();
//      if (ctx != null) {
//        view.getSettings().setAppCachePath(ctx.getCacheDir().getAbsolutePath());
//        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
//        view.getSettings().setAppCacheEnabled(true);
//      }
//    } else {
//      view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//      view.getSettings().setAppCacheEnabled(false);
//    }
  }

  @ReactProp(name = "cacheMode")
  public void setCacheMode(WebView view, String cacheModeString) {
    BPCReactProp.settingProp("cacheMode", cacheModeString, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("cacheMode", cacheModeString);
//    Integer cacheMode;
//    switch (cacheModeString) {
//      case "LOAD_CACHE_ONLY":
//        cacheMode = WebSettings.LOAD_CACHE_ONLY;
//        break;
//      case "LOAD_CACHE_ELSE_NETWORK":
//        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
//        break;
//      case "LOAD_NO_CACHE":
//        cacheMode = WebSettings.LOAD_NO_CACHE;
//        break;
//      case "LOAD_DEFAULT":
//      default:
//        cacheMode = WebSettings.LOAD_DEFAULT;
//        break;
//    }
//    view.getSettings().setCacheMode(cacheMode);
  }

  @ReactProp(name = "androidHardwareAccelerationDisabled")
  public void setHardwareAccelerationDisabled(WebView view, boolean disabled) {
    BPCReactProp.settingProp("androidHardwareAccelerationDisabled", disabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("androidHardwareAccelerationDisabled", disabled);
//    if (disabled) {
//      view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//    }
  }

  @ReactProp(name = "androidLayerType")
  public void setLayerType(WebView view, String layerTypeString) {
    BPCReactProp.settingProp("androidLayerType", layerTypeString, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("androidLayerType", layerTypeString);
//    int layerType = View.LAYER_TYPE_NONE;
//    switch (layerTypeString) {
//        case "hardware":
//          layerType = View.LAYER_TYPE_HARDWARE;
//          break;
//        case "software":
//          layerType = View.LAYER_TYPE_SOFTWARE;
//          break;
//    }
//    view.setLayerType(layerType, null);
  }


  @ReactProp(name = "overScrollMode")
  public void setOverScrollMode(WebView view, String overScrollModeString) {
    BPCReactProp.settingProp("overScrollMode", overScrollModeString, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("overScrollMode", overScrollModeString);
//    Integer overScrollMode;
//    switch (overScrollModeString) {
//      case "never":
//        overScrollMode = View.OVER_SCROLL_NEVER;
//        break;
//      case "content":
//        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS;
//        break;
//      case "always":
//      default:
//        overScrollMode = View.OVER_SCROLL_ALWAYS;
//        break;
//    }
//    view.setOverScrollMode(overScrollMode);
  }

  @ReactProp(name = "thirdPartyCookiesEnabled")
  public void setThirdPartyCookiesEnabled(WebView view, boolean enabled) {
    BPCReactProp.settingProp("thirdPartyCookiesEnabled", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("thirdPartyCookiesEnabled", enabled);
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//      CookieManager.getInstance().setAcceptThirdPartyCookies(view, enabled);
//    }
  }

  @ReactProp(name = "textZoom")
  public void setTextZoom(WebView view, int value) {
    BPCReactProp.settingProp("textZoom", value, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("textZoom", value);
//    view.getSettings().setTextZoom(value);
  }

  @ReactProp(name = "scalesPageToFit")
  public void setScalesPageToFit(WebView view, boolean enabled) {
    BPCReactProp.settingProp("scalesPageToFit", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("scalesPageToFit", enabled);
//    view.getSettings().setLoadWithOverviewMode(enabled);
//    view.getSettings().setUseWideViewPort(enabled);
  }

  @ReactProp(name = "domStorageEnabled")
  public void setDomStorageEnabled(WebView view, boolean enabled) {
    BPCReactProp.settingProp("domStorageEnabled", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("domStorageEnabled", enabled);
//    view.getSettings().setDomStorageEnabled(enabled);
  }

  @ReactProp(name = "userAgent")
  public void setUserAgent(WebView view, @Nullable String userAgent) {
    BPCReactProp.settingProp("userAgent", userAgent, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("userAgent", userAgent);
//    if (userAgent != null) {
//      mUserAgent = userAgent;
//    } else {
//      mUserAgent = null;
//    }
//    this.setUserAgentString(view);
  }

  @ReactProp(name = "applicationNameForUserAgent")
  public void setApplicationNameForUserAgent(WebView view, @Nullable String applicationName) {
    BPCReactProp.settingProp("applicationNameForUserAgent", applicationName, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("applicationNameForUserAgent", applicationName);
//    if(applicationName != null) {
//      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//        String defaultUserAgent = WebSettings.getDefaultUserAgent(view.getContext());
//        mUserAgentWithApplicationName = defaultUserAgent + " " + applicationName;
//      }
//    } else {
//      mUserAgentWithApplicationName = null;
//    }
//    this.setUserAgentString(view);
  }

//  protected void setUserAgentString(WebView view) {
//    if(mUserAgent != null) {
//      view.getSettings().setUserAgentString(mUserAgent);
//    } else if(mUserAgentWithApplicationName != null) {
//      view.getSettings().setUserAgentString(mUserAgentWithApplicationName);
//    } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//      // handle unsets of `userAgent` prop as long as device is >= API 17
//      view.getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(view.getContext()));
//    }
//  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @ReactProp(name = "mediaPlaybackRequiresUserAction")
  public void setMediaPlaybackRequiresUserAction(WebView view, boolean requires) {
    BPCReactProp.settingProp("mediaPlaybackRequiresUserAction", requires, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("mediaPlaybackRequiresUserAction", requires);

//    view.getSettings().setMediaPlaybackRequiresUserGesture(requires);
  }

  @ReactProp(name = "javaScriptCanOpenWindowsAutomatically")
  public void setJavaScriptCanOpenWindowsAutomatically(WebView view, boolean enabled) {
    BPCReactProp.settingProp("javaScriptCanOpenWindowsAutomatically", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("javaScriptCanOpenWindowsAutomatically", enabled);
//    view.getSettings().setJavaScriptCanOpenWindowsAutomatically(enabled);
  }

  @ReactProp(name = "allowFileAccessFromFileURLs")
  public void setAllowFileAccessFromFileURLs(WebView view, boolean allow) {
    BPCReactProp.settingProp("allowFileAccessFromFileURLs", allow, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("allowFileAccessFromFileURLs", allow);
//    view.getSettings().setAllowFileAccessFromFileURLs(allow);
  }

  @ReactProp(name = "allowUniversalAccessFromFileURLs")
  public void setAllowUniversalAccessFromFileURLs(WebView view, boolean allow) {
    BPCReactProp.settingProp("allowUniversalAccessFromFileURLs", allow, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("allowUniversalAccessFromFileURLs", allow);
//    view.getSettings().setAllowUniversalAccessFromFileURLs(allow);
  }

  @ReactProp(name = "saveFormDataDisabled")
  public void setSaveFormDataDisabled(WebView view, boolean disable) {
    BPCReactProp.settingProp("saveFormDataDisabled", disable, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("saveFormDataDisabled", disable);
//    view.getSettings().setSaveFormData(!disable);
  }

  @ReactProp(name = "injectedJavaScript")
  public void setInjectedJavaScript(WebView view, @Nullable String injectedJavaScript) {
    BPCReactProp.settingProp("injectedJavaScript", injectedJavaScript, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("injectedJavaScript", injectedJavaScript);
//    ((BPCWebView) view).setInjectedJavaScript(injectedJavaScript);
  }

//  @ReactProp(name = "startBootpay")
//  public void startBootpay(WebView view) {
//    BPCReactProp.settingProp("startBootpay", null, (BPCWebView) view);
//  }

  @ReactProp(name = "callJavaScript")
  public void callJavaScript(WebView view, @Nullable String injectedJavaScript) {
    BPCReactProp.settingProp("callJavaScript", injectedJavaScript, (BPCWebView) view);
  }

//  COMMAND_CALL_JAVASCRIPT

//  COMMAND_SET_JAVASCRIPT

  @ReactProp(name = "injectedJavaScriptBeforeContentLoaded")
  public void setInjectedJavaScriptBeforeContentLoaded(WebView view, @Nullable String injectedJavaScriptBeforeContentLoaded) {
    BPCReactProp.settingProp("injectedJavaScriptBeforeContentLoaded", injectedJavaScriptBeforeContentLoaded, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("injectedJavaScriptBeforeContentLoaded", injectedJavaScriptBeforeContentLoaded);
//    ((BPCWebView) view).setInjectedJavaScriptBeforeContentLoaded(injectedJavaScriptBeforeContentLoaded);
  }

  @ReactProp(name = "appendJavascriptBeforeContentLoaded")
  public void appendJavascriptBeforeContentLoaded(WebView view, @Nullable String injectedJavaScriptBeforeContentLoaded) {
    BPCReactProp.settingProp("appendJavascriptBeforeContentLoaded", injectedJavaScriptBeforeContentLoaded, (BPCWebView) view);
  }

  @ReactProp(name = "injectedJavaScriptForMainFrameOnly")
  public void setInjectedJavaScriptForMainFrameOnly(WebView view, boolean enabled) {
    BPCReactProp.settingProp("injectedJavaScriptForMainFrameOnly", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("injectedJavaScriptForMainFrameOnly", enabled);
//    ((BPCWebView) view).setInjectedJavaScriptForMainFrameOnly(enabled);
  }

  @ReactProp(name = "injectedJavaScriptBeforeContentLoadedForMainFrameOnly")
  public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(WebView view, boolean enabled) {
    BPCReactProp.settingProp("injectedJavaScriptBeforeContentLoadedForMainFrameOnly", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("injectedJavaScriptBeforeContentLoadedForMainFrameOnly", enabled);
//    ((BPCWebView) view).setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(enabled);
  }

  @ReactProp(name = "messagingEnabled")
  public void setMessagingEnabled(WebView view, boolean enabled) {
    BPCReactProp.settingProp("messagingEnabled", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("messagingEnabled", enabled);
//    ((BPCWebView) view).setMessagingEnabled(enabled);
  }

  @ReactProp(name = "messagingModuleName")
  public void setMessagingModuleName(WebView view, String moduleName) {
    BPCReactProp.settingProp("messagingModuleName", moduleName, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("messagingModuleName", moduleName);
//    ((BPCWebView) view).setMessagingModuleName(moduleName);
  }

  @ReactProp(name = "incognito")
  public void setIncognito(WebView view, boolean enabled) {
    BPCReactProp.settingProp("incognito", enabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("incognito", enabled);

    // Don't do anything when incognito is disabled
//    if (!enabled) {
//      return;
//    }
//
//    // Remove all previous cookies
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//      CookieManager.getInstance().removeAllCookies(null);
//    } else {
//      CookieManager.getInstance().removeAllCookie();
//    }
//
//    // Disable caching
//    view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//    view.getSettings().setAppCacheEnabled(false);
//    view.clearHistory();
//    view.clearCache(true);
//
//    // No form data or autofill enabled
//    view.clearFormData();
//    view.getSettings().setSavePassword(false);
//    view.getSettings().setSaveFormData(false);
  }

  @ReactProp(name = "source")
  public void setSource(WebView view, @Nullable ReadableMap source) {
    BPCReactProp.settingProp("source", source, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("source", source);
//    BPCReactProp.settingProp((BPCWebView) view);

//    if (source != null) {
//      if (source.hasKey("html")) {
//        String html = source.getString("html");
//        String baseUrl = source.hasKey("baseUrl") ? source.getString("baseUrl") : "";
//        view.loadDataWithBaseURL(baseUrl, html, HTML_MIME_TYPE, HTML_ENCODING, null);
//        return;
//      }
//      if (source.hasKey("uri")) {
//        String url = source.getString("uri");
//        String previousUrl = view.getUrl();
//        if (previousUrl != null && previousUrl.equals(url)) {
//          return;
//        }
//        if (source.hasKey("method")) {
//          String method = source.getString("method");
//          if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
//            byte[] postData = null;
//            if (source.hasKey("body")) {
//              String body = source.getString("body");
//              try {
//                postData = body.getBytes("UTF-8");
//              } catch (UnsupportedEncodingException e) {
//                postData = body.getBytes();
//              }
//            }
//            if (postData == null) {
//              postData = new byte[0];
//            }
//            view.postUrl(url, postData);
//            return;
//          }
//        }
//        HashMap<String, String> headerMap = new HashMap<>();
//        if (source.hasKey("headers")) {
//          ReadableMap headers = source.getMap("headers");
//          ReadableMapKeySetIterator iter = headers.keySetIterator();
//          while (iter.hasNextKey()) {
//            String key = iter.nextKey();
//            if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
//              if (view.getSettings() != null) {
//                view.getSettings().setUserAgentString(headers.getString(key));
//              }
//            } else {
//              headerMap.put(key, headers.getString(key));
//            }
//          }
//        }
//        view.loadUrl(url, headerMap);
//        return;
//      }
//    }
//    view.loadUrl(BLANK_URL);
  }

  @ReactProp(name = "onContentSizeChange")
  public void setOnContentSizeChange(WebView view, boolean sendContentSizeChangeEvents) {
    BPCReactProp.settingProp("onContentSizeChange", sendContentSizeChangeEvents, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("onContentSizeChange", sendContentSizeChangeEvents);
//    ((BPCWebView) view).setSendContentSizeChangeEvents(sendContentSizeChangeEvents);
  }

  @ReactProp(name = "mixedContentMode")
  public void setMixedContentMode(WebView view, @Nullable String mixedContentMode) {
    BPCReactProp.settingProp("mixedContentMode", mixedContentMode, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("mixedContentMode", mixedContentMode);

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//      if (mixedContentMode == null || "never".equals(mixedContentMode)) {
//        view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
//      } else if ("always".equals(mixedContentMode)) {
//        view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//        CookieManager.getInstance().setAcceptCookie(true);
//        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
//      } else if ("compatibility".equals(mixedContentMode)) {
//        view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
//      }
//    }
  }


  @ReactProp(name = "urlPrefixesForDefaultIntent")
  public void setUrlPrefixesForDefaultIntent(
          WebView view,
          @Nullable ReadableArray urlPrefixesForDefaultIntent) {
    BPCReactProp.settingProp("urlPrefixesForDefaultIntent", urlPrefixesForDefaultIntent, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("urlPrefixesForDefaultIntent", urlPrefixesForDefaultIntent);

//    BPCWebViewClient client = ((BPCWebView) view).getBPCWebViewClient();
//    if (client != null && urlPrefixesForDefaultIntent != null) {
//      client.setUrlPrefixesForDefaultIntent(urlPrefixesForDefaultIntent);
//    }
  }


  @ReactProp(name = "allowsFullscreenVideo")
  public void setAllowsFullscreenVideo(
    WebView view,
    @Nullable Boolean allowsFullscreenVideo) {
    BPCReactProp.settingProp("allowsFullscreenVideo", allowsFullscreenVideo, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("allowsFullscreenVideo", allowsFullscreenVideo);
//    mAllowsFullscreenVideo = allowsFullscreenVideo != null && allowsFullscreenVideo;
//    setupWebChromeClient((ReactContext)view.getContext(), view);
  }

  @ReactProp(name = "allowFileAccess")
  public void setAllowFileAccess(
    WebView view,
    @Nullable Boolean allowFileAccess) {
    BPCReactProp.settingProp("allowFileAccess", allowFileAccess, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("allowFileAccess", allowFileAccess);
//    view.getSettings().setAllowFileAccess(allowFileAccess != null && allowFileAccess);
  }

  @ReactProp(name = "geolocationEnabled")
  public void setGeolocationEnabled(
    WebView view,
    @Nullable Boolean isGeolocationEnabled) {
    BPCReactProp.settingProp("geolocationEnabled", isGeolocationEnabled, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("geolocationEnabled", isGeolocationEnabled);
//    view.getSettings().setGeolocationEnabled(isGeolocationEnabled != null && isGeolocationEnabled);
  }

  @ReactProp(name = "onScroll")
  public void setOnScroll(WebView view, boolean hasScrollEvent) {
    BPCReactProp.settingProp("onScroll", hasScrollEvent, (BPCWebView) view);
//    BPCReactProp.getInstance().map.put("onScroll", hasScrollEvent);
//    ((BPCWebView) view).setHasScrollEvent(hasScrollEvent);
  }

  @Override
  protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
    // Do not register default touch emitter and let WebView implementation handle touches
    view.setWebViewClient(new BPCWebViewClient());
  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    Map export = super.getExportedCustomDirectEventTypeConstants();
    if (export == null) {
      export = MapBuilder.newHashMap();
    }
    export.put(TopLoadingProgressEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingProgress"));
    export.put(TopShouldStartLoadWithRequestEvent.EVENT_NAME, MapBuilder.of("registrationName", "onShouldStartLoadWithRequest"));
    export.put(ScrollEventType.getJSEventName(ScrollEventType.SCROLL), MapBuilder.of("registrationName", "onScroll"));
    export.put(TopHttpErrorEvent.EVENT_NAME, MapBuilder.of("registrationName", "onHttpError"));
    export.put(TopRenderProcessGoneEvent.EVENT_NAME, MapBuilder.of("registrationName", "onRenderProcessGone"));
    return export;
  }

  @Override
  public @Nullable
  Map<String, Integer> getCommandsMap() {
    return MapBuilder.<String, Integer>builder()
      .put("goBack", COMMAND_GO_BACK)
      .put("goForward", COMMAND_GO_FORWARD)
      .put("reload", COMMAND_RELOAD)
      .put("stopLoading", COMMAND_STOP_LOADING)
      .put("postMessage", COMMAND_POST_MESSAGE)
      .put("injectJavaScript", COMMAND_INJECT_JAVASCRIPT)
      .put("callJavaScript", COMMAND_CALL_JAVASCRIPT)
      .put("startBootpay", COMMAND_START_BOOTPAY)
      .put("appendJavaScriptBeforeContentLoaded", COMMAND_APPEND_BEFORE_JAVASCRIPT)
      .put("loadUrl", COMMAND_LOAD_URL)
      .put("requestFocus", COMMAND_FOCUS)
      .put("clearFormData", COMMAND_CLEAR_FORM_DATA)
      .put("clearCache", COMMAND_CLEAR_CACHE)
      .put("clearHistory", COMMAND_CLEAR_HISTORY)
      .build();
  }

  @Override
  public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
    switch (commandId) {
      case COMMAND_GO_BACK:
        root.goBack();
        break;
      case COMMAND_GO_FORWARD:
        root.goForward();
        break;
      case COMMAND_RELOAD:
        root.reload();
        break;
      case COMMAND_STOP_LOADING:
        root.stopLoading();
        break;
      case COMMAND_POST_MESSAGE:
        try {
          BPCWebView reactWebView = (BPCWebView) root;
          JSONObject eventInitDict = new JSONObject();
          eventInitDict.put("data", args.getString(0));
          reactWebView.evaluateJavascriptWithFallback("(function () {" +
            "var event;" +
            "var data = " + eventInitDict.toString() + ";" +
            "try {" +
            "event = new MessageEvent('message', data);" +
            "} catch (e) {" +
            "event = document.createEvent('MessageEvent');" +
            "event.initMessageEvent('message', true, true, data.data, data.origin, data.lastEventId, data.source);" +
            "}" +
            "document.dispatchEvent(event);" +
            "})();");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        break;
      case COMMAND_CALL_JAVASCRIPT:
        BPCWebView reactWebView1 = (BPCWebView) root;
        reactWebView1.evaluateJavascriptWithFallback(args.getString(0));
        break;
      case COMMAND_INJECT_JAVASCRIPT:
        BPCWebView reactWebView2 = (BPCWebView) root;
        reactWebView2.setInjectedJavaScript(args.getString(0));
        break;
      case COMMAND_APPEND_BEFORE_JAVASCRIPT:
        BPCWebView reactWebView3 = (BPCWebView) root;
        reactWebView3.appendJavaScriptBeforeContentLoaded(args.getString(0));
        break;
      case COMMAND_START_BOOTPAY:
        BPCWebView reactWebView4 = (BPCWebView) root;
        reactWebView4.startBootpay();
        break;
      case COMMAND_LOAD_URL:
        if (args == null) {
          throw new RuntimeException("Arguments for loading an url are null!");
        }
        ((BPCWebView) root).progressChangedFilter.setWaitingForCommandLoadUrl(false);
        root.loadUrl(args.getString(0));
        break;
      case COMMAND_FOCUS:
        root.requestFocus();
        break;
      case COMMAND_CLEAR_FORM_DATA:
        root.clearFormData();
        break;
      case COMMAND_CLEAR_CACHE:
        boolean includeDiskFiles = args != null && args.getBoolean(0);
        root.clearCache(includeDiskFiles);
        break;
      case COMMAND_CLEAR_HISTORY:
        root.clearHistory();
        break;
    }
  }

  @Override
  public void onDropViewInstance(WebView webView) {
    super.onDropViewInstance(webView);
    ((ThemedReactContext) webView.getContext()).removeLifecycleEventListener((BPCWebView) webView);
    ((BPCWebView) webView).cleanupCallbacksAndDestroy();
    ((BPCWebView) webView).setWebChromeClient(null);
//    mWebChromeClient = null;
  }

  public static BPCWebViewModule GetModule(ReactContext reactContext) {
    return reactContext.getNativeModule(BPCWebViewModule.class);
  }

//  protected void setupWebChromeClient(ReactContext reactContext, WebView webView) {
//    if (mAllowsFullscreenVideo) {
//      int initialRequestedOrientation = reactContext.getCurrentActivity().getRequestedOrientation();
//      mWebChromeClient = new BPCWebChromeClient(reactContext, webView) {
//        @Override
//        public Bitmap getDefaultVideoPoster() {
//          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
//        }
//
//        @Override
//        public void onShowCustomView(View view, CustomViewCallback callback) {
//          if (mVideoView != null) {
//            callback.onCustomViewHidden();
//            return;
//          }
//
//          mVideoView = view;
//          mCustomViewCallback = callback;
//
//          mReactContext.getCurrentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//
//          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
//            mReactContext.getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//          }
//
//          mVideoView.setBackgroundColor(Color.BLACK);
//          getRootView().addView(mVideoView, FULLSCREEN_LAYOUT_PARAMS);
//          mWebView.setVisibility(View.GONE);
//
//          mReactContext.addLifecycleEventListener(this);
//        }
//
//        @Override
//        public void onHideCustomView() {
//          if (mVideoView == null) {
//            return;
//          }
//
//          mVideoView.setVisibility(View.GONE);
//          getRootView().removeView(mVideoView);
//          mCustomViewCallback.onCustomViewHidden();
//
//          mVideoView = null;
//          mCustomViewCallback = null;
//
//          mWebView.setVisibility(View.VISIBLE);
//
//          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mReactContext.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//          }
//          mReactContext.getCurrentActivity().setRequestedOrientation(initialRequestedOrientation);
//
//          mReactContext.removeLifecycleEventListener(this);
//        }
//      };
//      webView.setWebChromeClient(mWebChromeClient);
//    } else {
//      if (mWebChromeClient != null) {
//        mWebChromeClient.onHideCustomView();
//      }
//      mWebChromeClient = new BPCWebChromeClient(reactContext, webView) {
//        @Override
//        public Bitmap getDefaultVideoPoster() {
//          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
//        }
//      };
//      webView.setWebChromeClient(mWebChromeClient);
//    }
//  }

  private static boolean getAllowsFullScreenVideo() {
    Object val = BPCReactProp.getInstance().map.get("allowsFullscreenVideo");
    if(val != null) return (boolean) val;
    return false;
  }

  public static void SetupWebChromeClient(ReactContext reactContext, WebView webView) {
    final boolean allowsFullscreenVideo = getAllowsFullScreenVideo();
    int initialRequestedOrientation = reactContext.getCurrentActivity().getRequestedOrientation();
    BPCWebChromeClient chromeClient = new BPCWebChromeClient(reactContext, webView) {
      @Override
      public Bitmap getDefaultVideoPoster() {
        return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
      }

      @Override
      public void onShowCustomView(View view, CustomViewCallback callback) {

        if (mVideoView != null) {
          callback.onCustomViewHidden();
          return;
        }

        mVideoView = view;
        mCustomViewCallback = callback;

        mReactContext.getCurrentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
          mReactContext.getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        mVideoView.setBackgroundColor(Color.BLACK);
        getRootView().addView(mVideoView, FULLSCREEN_LAYOUT_PARAMS);
        mWebView.setVisibility(View.GONE);

        mReactContext.addLifecycleEventListener(this);
      }

      @Override
      public void onHideCustomView() {
        if (mVideoView == null) {
          return;
        }

        mVideoView.setVisibility(View.GONE);
        getRootView().removeView(mVideoView);
        mCustomViewCallback.onCustomViewHidden();

        mVideoView = null;
        mCustomViewCallback = null;

        mWebView.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          mReactContext.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        mReactContext.getCurrentActivity().setRequestedOrientation(initialRequestedOrientation);

        mReactContext.removeLifecycleEventListener(this);
      }
    };
    webView.setWebChromeClient(chromeClient);

  }
}
