package kr.co.bootpay;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BPCReactProp {
    Map<String, Object> map;
    String mUserAgent;
    String mUserAgentWithApplicationName;

    private BPCReactProp() {
        this.map = new HashMap<>();
    }

    /**
     * SingletonHolder is loaded on the first execution of Singleton.getInstance()
     * or the first access to SingletonHolder.INSTANCE, not before.
     */
    private static class SingletonHolder {
        public static final BPCReactProp INSTANCE = new BPCReactProp();
    }
    public static BPCReactProp getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static BPCWebView settingPropAll(BPCWebView webview) {
        for(String key : getInstance().map.keySet()) {
            settingPropFor(key, webview);
        }
        return webview;
    }

    public static void settingProp(String key, Object val, BPCWebView view) {
        getInstance().map.put(key, val);
        if(view != null) settingPropFor(key, view);
    }

    private static void settingPropFor(String key, BPCWebView view) {
        Object val = getInstance().map.get(key);
        if(val == null) return;
        switch (key) {
            case "javaScriptEnabled":
                view.getSettings().setJavaScriptEnabled((boolean) val);
                break;
            case "setSupportMultipleWindows":
                view.getSettings().setSupportMultipleWindows((boolean) val);
                break;
            case "showsHorizontalScrollIndicator":
                view.setHorizontalScrollBarEnabled((boolean) val);
                break;
            case "showsVerticalScrollIndicator":
                view.setVerticalScrollBarEnabled((boolean) val);
                break;
            case "cacheEnabled":
                setCacheEnabled(view, (boolean) val);
                break;
            case "cacheMode":
                setCacheMode(view, (String) val);
                break;
            case "androidHardwareAccelerationDisabled":
                if ((boolean) val) view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                break;
            case "androidLayerType":
                setAndroidLayerType(view, (String) val);
                break;
            case "overScrollMode":
                setOverScrollMode(view, (String) val);
                break;
            case "thirdPartyCookiesEnabled":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) CookieManager.getInstance().setAcceptThirdPartyCookies(view, (boolean) val);
                break;
            case "textZoom":
                view.getSettings().setTextZoom((int) val);
                break;
            case "scalesPageToFit":
                view.getSettings().setLoadWithOverviewMode((boolean) val);
                view.getSettings().setUseWideViewPort((boolean) val);
                break;
            case "domStorageEnabled":
                view.getSettings().setDomStorageEnabled((boolean) val);
                break;
            case "userAgent":
                setUserAgent(view, (String) val);
                break;
            case "applicationNameForUserAgent":
                setApplicationNameForUserAgent(view, (String) val);
                break;
            case "mediaPlaybackRequiresUserAction":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    view.getSettings().setMediaPlaybackRequiresUserGesture((boolean) val);
                }
                break;
            case "javaScriptCanOpenWindowsAutomatically":
                view.getSettings().setJavaScriptCanOpenWindowsAutomatically((boolean) val);
                break;
            case "allowFileAccessFromFileURLs":
                view.getSettings().setAllowFileAccessFromFileURLs((boolean) val);
                break;
            case "allowUniversalAccessFromFileURLs":
                view.getSettings().setAllowUniversalAccessFromFileURLs((boolean) val);
                break;
            case "saveFormDataDisabled":
                view.getSettings().setSaveFormData(!(boolean) val);
                break;
            case "injectedJavaScript":
                view.setInjectedJavaScript((String) val);
                break;
            case "injectedJavaScriptBeforeContentLoaded":
                view.setInjectedJavaScriptBeforeContentLoaded((String) val);
                break;

            case "injectedJavaScriptForMainFrameOnly":
                view.setInjectedJavaScriptForMainFrameOnly((boolean) val);
                break;
            case "messagingEnabled":
                view.setMessagingEnabled((boolean) val);
                break;
            case "messagingModuleName":
                view.setMessagingModuleName((String) val);
                break;
            case "incognito":
                setIncognito(view, (boolean) val);
                break;
            case "source":
                setSource(view, (ReadableMap) val);
                break;
        }
    }

    private static void setSource(BPCWebView view, ReadableMap val) {
        final String HTML_ENCODING = "UTF-8";
        final String HTML_MIME_TYPE = "text/html";
        String HTTP_METHOD_POST = "POST";
        // Use `webView.loadUrl("about:blank")` to reliably reset the view
        // state and release page resources (including any running JavaScript).
        final String BLANK_URL = "about:blank";

        ReadableMap source = val;
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                String baseUrl = source.hasKey("baseUrl") ? source.getString("baseUrl") : "";
                view.loadDataWithBaseURL(baseUrl, html, HTML_MIME_TYPE, HTML_ENCODING, null);
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                String previousUrl = view.getUrl();
                if (previousUrl != null && previousUrl.equals(url)) {
                    return;
                }
                if (source.hasKey("method")) {
                    String method = source.getString("method");
                    if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
                        byte[] postData = null;
                        if (source.hasKey("body")) {
                            String body = source.getString("body");
                            try {
                                postData = body.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                postData = body.getBytes();
                            }
                        }
                        if (postData == null) {
                            postData = new byte[0];
                        }
                        view.postUrl(url, postData);
                        return;
                    }
                }
                HashMap<String, String> headerMap = new HashMap<>();
                if (source.hasKey("headers")) {
                    ReadableMap headers = source.getMap("headers");
                    ReadableMapKeySetIterator iter = headers.keySetIterator();
                    while (iter.hasNextKey()) {
                        String key = iter.nextKey();
                        if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
                            if (view.getSettings() != null) {
                                view.getSettings().setUserAgentString(headers.getString(key));
                            }
                        } else {
                            headerMap.put(key, headers.getString(key));
                        }
                    }
                }
                view.loadUrl(url, headerMap);
                return;
            }
        }
        view.loadUrl(BLANK_URL);
    }

    private static void setIncognito(BPCWebView view, boolean val) {
        if (!val) {
            return;
        }

        // Remove all previous cookies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
        } else {
            CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        view.getSettings().setAppCacheEnabled(false);
        view.clearHistory();
        view.clearCache(true);

        // No form data or autofill enabled
        view.clearFormData();
        view.getSettings().setSavePassword(false);
        view.getSettings().setSaveFormData(false);
    }

    private static void setApplicationNameForUserAgent(BPCWebView view, String val) {
        if(val != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                String defaultUserAgent = WebSettings.getDefaultUserAgent(view.getContext());
                getInstance().mUserAgentWithApplicationName = defaultUserAgent + " " + val;
            }
        } else {
            getInstance().mUserAgentWithApplicationName = null;
        }
        setUserAgentString(view);
    }

    private static void setUserAgent(BPCWebView view, String val) {
        if (val != null) {
            getInstance().mUserAgent = val;
        } else {
            getInstance().mUserAgent = null;
        }
        setUserAgentString(view);
    }

    private static void setOverScrollMode(BPCWebView view, String val) {
        Integer overScrollMode;
        switch (val) {
            case "never":
                overScrollMode = View.OVER_SCROLL_NEVER;
                break;
            case "content":
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS;
                break;
            case "always":
            default:
                overScrollMode = View.OVER_SCROLL_ALWAYS;
                break;
        }
        view.setOverScrollMode(overScrollMode);
    }

    private static void setAndroidLayerType(BPCWebView view, String val) {
        int layerType = View.LAYER_TYPE_NONE;
        switch (val) {
            case "hardware":
                layerType = View.LAYER_TYPE_HARDWARE;
                break;
            case "software":
                layerType = View.LAYER_TYPE_SOFTWARE;
                break;
        }
        view.setLayerType(layerType, null);
    }

    private static void setCacheMode(BPCWebView view, String val) {
        Integer cacheMode;
        switch (val) {
            case "LOAD_CACHE_ONLY":
                cacheMode = WebSettings.LOAD_CACHE_ONLY;
                break;
            case "LOAD_CACHE_ELSE_NETWORK":
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
                break;
            case "LOAD_NO_CACHE":
                cacheMode = WebSettings.LOAD_NO_CACHE;
                break;
            case "LOAD_DEFAULT":
            default:
                cacheMode = WebSettings.LOAD_DEFAULT;
                break;
        }
        view.getSettings().setCacheMode(cacheMode);
    }

    private static void setCacheEnabled(BPCWebView view, boolean val) {
        if (val) {
            Context ctx = view.getContext();
            if (ctx != null) {
                view.getSettings().setAppCachePath(ctx.getCacheDir().getAbsolutePath());
                view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                view.getSettings().setAppCacheEnabled(true);
            }
        } else {
            view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            view.getSettings().setAppCacheEnabled(false);
        }
    }

    private static void setUserAgentString(BPCWebView view) {
        if(getInstance().mUserAgent != null) {
            view.getSettings().setUserAgentString(getInstance().mUserAgent);
        } else if(getInstance().mUserAgentWithApplicationName != null) {
            view.getSettings().setUserAgentString(getInstance().mUserAgentWithApplicationName);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // handle unsets of `userAgent` prop as long as device is >= API 17
            view.getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(view.getContext()));
        }
    }
}
