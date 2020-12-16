package com.bootpaytemp;

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext


class BPCWebViewPackage: ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext) = listOf(
    BPCWebViewModule(reactContext)
  )

  override fun createViewManagers(reactContext: ReactApplicationContext) = listOf(
    BPCWebViewManager()
  )
}
