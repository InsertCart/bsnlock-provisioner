package com.emilock.provisioner.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log

/**
 * OwnershipTransferManager
 *
 * Transfers Device Owner from the Provisioner app to EmiLock.
 *
 * After transfer:
 *  - EmiLock becomes Device Owner (full control)
 *  - Provisioner loses Device Owner but stays installed as a "companion" admin
 *  - Provisioner's onTransferOwnershipComplete() fires to handle cleanup
 *
 * Requires: API 28 (Android 9)+
 * Both apps MUST have a DeviceAdminReceiver declared in their manifest.
 */
object OwnershipTransferManager {

    private const val TAG = "EmiLock.Transfer"

    /**
     * Transfer Device Owner from Provisioner → EmiLock.
     *
     * @return true if transfer succeeded, false otherwise
     */
    fun transferToEmiLock(
        context: Context,
        dpm: DevicePolicyManager
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.e(TAG, "Device Owner transfer requires API 28+. Current: ${Build.VERSION.SDK_INT}")
            return false
        }

        val fromAdmin = ComponentName(
            context.packageName,
            "com.emilock.provisioner.receivers.ProvisionerAdminReceiver"
        )

        val toAdmin = ComponentName(
            ProvisionerConfig.EMILOCK_PACKAGE,
            ProvisionerConfig.EMILOCK_ADMIN_RECEIVER
        )

        // Verify EmiLock is installed
        return try {
            context.packageManager.getPackageInfo(ProvisionerConfig.EMILOCK_PACKAGE, 0)
            Log.d(TAG, "EmiLock package found — proceeding with ownership transfer")

            // Pass any data EmiLock needs after taking ownership
            val bundle = PersistableBundle().apply {
                putString("transferred_from", "emilock_provisioner")
                putLong("transfer_timestamp", System.currentTimeMillis())
                // Add any other metadata EmiLock needs
            }

            // ── THE OWNERSHIP TRANSFER ───────────────────────────────────────
            dpm.transferOwnership(fromAdmin, toAdmin, bundle)
            // After this call returns successfully, EmiLock is the Device Owner.
            // onTransferOwnershipComplete() will fire on EmiLock's admin receiver.

            Log.d(TAG, "✅ Ownership transferred to EmiLock successfully")
            true

        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            Log.e(TAG, "❌ EmiLock not installed — cannot transfer ownership")
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ EmiLock's DeviceAdminReceiver is not active — it must be enabled first")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Transfer failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Activate EmiLock's DeviceAdminReceiver as an active admin BEFORE transferring ownership.
     * The target admin MUST be active before transferOwnership() is called.
     *
     * As Device Owner we can activate another app's admin silently via:
     *   DevicePolicyManager.setActiveAdmin (not available to DO directly)
     *
     * Instead, we use the PackageManager to trigger it indirectly, OR
     * we rely on EmiLock declaring itself and auto-activating via its own
     * DeviceAdminReceiver.onEnabled() which fires when the system activates it.
     *
     * The correct flow is:
     * 1. Install EmiLock
     * 2. EmiLock must call dpm.isAdminActive() — if not, it requests admin activation
     * 3. As DO we can grant it admin silently using the hidden API or by
     *    using ACTION_ADD_DEVICE_ADMIN with auto-approval (not available to 3rd party)
     *
     * SIMPLEST SOLUTION: EmiLock activates its own admin on first launch (MainActivity).
     * Then Provisioner calls transferOwnership().
     *
     * Alternative: Use DevicePolicyManager.getParentProfileInstance on API 30+ for silent activation.
     */
    fun checkEmiLockAdminActive(context: Context, dpm: DevicePolicyManager): Boolean {
        val toAdmin = ComponentName(
            ProvisionerConfig.EMILOCK_PACKAGE,
            ProvisionerConfig.EMILOCK_ADMIN_RECEIVER
        )
        return try {
            dpm.isAdminActive(toAdmin)
        } catch (_: Exception) { false }
    }
}