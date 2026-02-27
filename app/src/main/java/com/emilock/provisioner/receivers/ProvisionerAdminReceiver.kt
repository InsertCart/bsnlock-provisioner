package com.emilock.provisioner.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import com.emilock.provisioner.ui.ProvisioningCompleteActivity
import com.emilock.provisioner.utils.PermissionGranter
import com.emilock.provisioner.utils.ProvisionerConfig

/**
 * ProvisionerAdminReceiver
 *
 * This is the Device Policy Controller (DPC) entry point.
 * When Android Setup Wizard finishes QR/NFC provisioning, it:
 *  1. Installs this APK
 *  2. Sets this component as Device Owner
 *  3. Fires onProfileProvisioningComplete
 *  4. Launches the activity registered for ACTION_MANAGED_DEVICE_SETUP_ACTION
 *
 * From here we silently grant all permissions and prepare EmiLock installation.
 */
class ProvisionerAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "EmiLock.Provisioner"

        fun getComponentName(context: Context): ComponentName =
            ComponentName(context, ProvisionerAdminReceiver::class.java)
    }

    /**
     * Called IMMEDIATELY after the device is provisioned as a Managed Device.
     * This is the earliest point we can act as Device Owner.
     *
     * NOTE: At this point the device setup wizard is still showing.
     * We apply restrictions and grant permissions here silently.
     * Then launch ProvisioningCompleteActivity to handle UI + EmiLock install.
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d(TAG, "✅ PROVISIONING COMPLETE — Device Owner is active")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getComponentName(context)

        // ── Step 1: Apply immediate Device Owner lockdown ────────────────
        applyInitialRestrictions(dpm, admin, context)

        // ── Step 2: Silently grant all runtime permissions to THIS app ───
        PermissionGranter.grantAllToSelf(dpm, admin, context)

        // ── Step 3: Read provisioning extras (passed via QR code) ────────
        val extras: PersistableBundle? = dpm.getTransferOwnershipAdminExtras(admin)
        val emiLockDownloadUrl = extras?.getString(ProvisionerConfig.KEY_EMILOCK_APK_URL)
            ?: ProvisionerConfig.DEFAULT_EMILOCK_APK_URL

        Log.d(TAG, "EmiLock APK URL: $emiLockDownloadUrl")

        // ── Step 4: Launch setup activity (shown to user during wizard) ──
        val setupIntent = Intent(context, ProvisioningCompleteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ProvisionerConfig.KEY_EMILOCK_APK_URL, emiLockDownloadUrl)
        }
        context.startActivity(setupIntent)
    }

    /**
     * Apply initial Device Owner restrictions immediately after provisioning.
     * No UI, runs silently in the background.
     */
    private fun applyInitialRestrictions(
        dpm: DevicePolicyManager,
        admin: ComponentName,
        context: Context
    ) {
        try {
            // Block factory reset — most critical restriction
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_FACTORY_RESET)
            // Block safe boot to prevent bypassing our service
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_SAFE_BOOT)
            // Prevent USB file transfer (stops APK sideloading to bypass)
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
            // Block adding new accounts
            dpm.addUserRestriction(admin, android.os.UserManager.DISALLOW_MODIFY_ACCOUNTS)
            // Keep screen on during provisioning flow
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