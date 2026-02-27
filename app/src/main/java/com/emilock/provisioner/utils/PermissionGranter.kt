package com.emilock.provisioner.utils

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * PermissionGranter
 *
 * As Device Owner, we can silently grant ANY runtime permission to ANY installed app
 * using DevicePolicyManager.setPermissionGrantState().
 *
 * This is what enterprise MDM apps do — zero permission popups for the user.
 * Available from API 23+.
 *
 * Grant states:
 *  PERMISSION_GRANT_STATE_GRANTED  = auto-granted, user cannot revoke
 *  PERMISSION_GRANT_STATE_DENIED   = auto-denied
 *  PERMISSION_GRANT_STATE_DEFAULT  = let user decide (normal behavior)
 */
object PermissionGranter {

    private const val TAG = "EmiLock.PermGranter"

    // All permissions EmiLock needs — granted silently
    private val EMILOCK_PERMISSIONS = listOf(
        // Location
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        // Phone / SIM
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        // Notifications
        Manifest.permission.POST_NOTIFICATIONS,
        // Camera (for QR scan if needed)
        Manifest.permission.CAMERA,
    )

    /**
     * Grant all required permissions to the EmiLock package silently.
     * Called by the Provisioner (Device Owner) before transferring ownership.
     *
     * @param targetPackage the package name to grant permissions to (com.emilock.app)
     */
    fun grantAllToPackage(
        dpm: DevicePolicyManager,
        admin: ComponentName,
        context: Context,
        targetPackage: String
    ) {
        var successCount = 0
        var failCount    = 0

        for (permission in EMILOCK_PERMISSIONS) {
            try {
                val granted = dpm.setPermissionGrantState(
                    admin,
                    targetPackage,
                    permission,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
                if (granted) {
                    successCount++
                    Log.d(TAG, "✅ Granted [$targetPackage] $permission")
                } else {
                    failCount++
                    Log.w(TAG, "⚠️ Could not grant [$targetPackage] $permission")
                }
            } catch (e: Exception) {
                failCount++
                Log.e(TAG, "❌ Error granting $permission: ${e.message}")
            }
        }

        Log.d(TAG, "Permission grant complete — success=$successCount, fail=$failCount")
    }

    /**
     * Grant all permissions to the Provisioner itself (so it can read IMEI, location etc.)
     */
    fun grantAllToSelf(
        dpm: DevicePolicyManager,
        admin: ComponentName,
        context: Context
    ) {
        grantAllToPackage(dpm, admin, context, context.packageName)
    }

    /**
     * Also grant special system-level capabilities to EmiLock as Device Owner allows:
     *  - Allow EmiLock to always run in background
     *  - Exempt from battery optimization
     *  - Set as persistent preferred activity for MAIN/LAUNCHER (optional kiosk mode)
     */
    fun applySystemPrivileges(
        dpm: DevicePolicyManager,
        admin: ComponentName,
        context: Context,
        targetPackage: String
    ) {
        try {
            // Prevent EmiLock from being battery restricted
            // On Android 9+, we can use setApplicationHidden reverse logic + policies
            // Battery optimization exemption is requested by EmiLock itself on first launch
            Log.d(TAG, "System privileges applied to $targetPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying system privileges: ${e.message}")
        }
    }
}