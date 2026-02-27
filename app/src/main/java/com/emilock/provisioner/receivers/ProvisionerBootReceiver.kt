package com.emilock.provisioner.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ProvisionerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("EmiLock.Provisioner", "Boot completed — ensuring EmiLock is running")

        // Launch EmiLock if installed
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage("com.emilock.app")
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            launchIntent?.let { context.startActivity(it) }
        } catch (e: Exception) {
            Log.e("EmiLock.Provisioner", "Could not launch EmiLock: ${e.message}")
        }
    }
}