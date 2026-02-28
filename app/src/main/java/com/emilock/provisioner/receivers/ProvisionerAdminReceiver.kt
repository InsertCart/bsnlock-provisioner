package com.emilock.provisioner.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import com.emilock.provisioner.ui.ProvisioningCompleteActivity
import com.emilock.provisioner.utils.PermissionGranter
import com.emilock.provisioner.utils.ProvisionerConfig

class ProvisionerAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "EmiLock.Provisioner"

        fun getComponentName(context: Context): ComponentName =
            ComponentName(context, ProvisionerAdminReceiver::class.java)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d(TAG, "✅ PROVISIONING COMPLETE — Device Owner is active")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getComponentName(context)

        // ── Step 1: Apply immediate Device Owner lockdown ────────────────
        applyInitialRestrictions(dpm, admin)

        // ── Step 2: Silently grant all runtime permissions to THIS app ───
        PermissionGranter.grantAllToSelf(dpm, admin, context)

        // ── Step 3: Read provisioning extras passed via QR code ──────────
        // CORRECT API: extras come from the intent, not from dpm
        val extras: PersistableBundle? = intent.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
        )
        val emiLockDownloadUrl = extras?.getString(ProvisionerConfig.KEY_EMILOCK_APK_URL)
            ?: ProvisionerConfig.DEFAULT_EMILOCK_APK_URL

        Log.d(TAG, "EmiLock APK URL: $emiLockDownloadUrl")

        // ── Step 4: Launch setup activity ───────────────────────────────
        val setupIntent = Intent(context, ProvisioningCompleteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ProvisionerConfig.KEY_EMILOCK_APK_URL, emiLockDownloadUrl)
        }
        context.startActivity(setupIntent)
    }

    private fun applyInitialRestrictions(dpm: DevicePolicyManager, admin: ComponentName) {
        try {
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_MODIFY_ACCOUNTS)
            dpm.setMaximumTimeToLock(admin, 0)
            Log.d(TAG, "Initial DO restrictions applied")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying restrictions: ${e.message}")
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }
}