package com.vyrena.mconbridge

import android.app.Application

class MconBridgeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
