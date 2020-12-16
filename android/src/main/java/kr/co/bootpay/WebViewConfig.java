package kr.co.bootpay;

import android.webkit.WebView;

/**
 * Implement this interface in order to config your {@link WebView}. An instance of that
 * implementation will have to be given as a constructor argument to {@link BPCWebViewManager}.
 */
public interface WebViewConfig {

  void configWebView(WebView webView);
}
