package com.bootpaytemp;

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import kr.co.bootpay.BPCWebViewManager
import kr.co.bootpay.BPCWebViewModule


class BootpayPackage: ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext) = listOf(
    BPCWebViewModule(reactContext)
  )

  override fun createViewManagers(reactContext: ReactApplicationContext) = listOf(
    BPCWebViewManager()
  )
}
