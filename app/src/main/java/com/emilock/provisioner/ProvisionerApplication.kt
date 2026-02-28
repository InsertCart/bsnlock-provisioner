package com.emilock.provisioner

import android.app.Application
import android.util.Log

/**
 * ProvisionerApplication
 *
 * Minimal Application class for the Provisioner.
 * Referenced in AndroidManifest: android:name=".ProvisionerApplication"
 * Without this file the app crashes immediately on launch.
 */
class ProvisionerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("EmiLock.Provisioner", "ProvisionerApplication started")
        // No heavy init needed — Provisioner is a bootstrap-only app
    }
}