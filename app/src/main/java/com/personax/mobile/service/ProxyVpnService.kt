package com.personax.mobile.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class ProxyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val proxy = intent?.getStringExtra("proxy") ?: return START_NOT_STICKY

        vpnInterface = Builder()
            .setSession("PersonaX Mobile")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .establish()

        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
