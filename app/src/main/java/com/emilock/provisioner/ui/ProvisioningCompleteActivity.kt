package com.emilock.provisioner.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.emilock.provisioner.receivers.ProvisionerAdminReceiver
import com.emilock.provisioner.utils.PermissionGranter

/**
 * ProvisioningCompleteActivity
 *
 * Shown briefly during Setup Wizard after QR provisioning completes.
 * NEW FLOW: We no longer download EmiLock here.
 *
 * We just:
 * 1. Apply Device Owner restrictions
 * 2. Call setResult(RESULT_OK) → Setup Wizard finishes → phone boots normally
 * 3. Dealer then installs EmiLock manually (browser download or APK)
 * 4. Dealer opens THIS app → TransferActivity → taps Transfer button
 */
class ProvisioningCompleteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmiLock.SetupActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Provisioning complete — finishing setup wizard")

        val dpm   = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ProvisionerAdminReceiver.getComponentName(this)

        // Grant all permissions to ourselves silently while we are Device Owner
        PermissionGranter.grantAllToSelf(dpm, admin, this)

        // Tell Setup Wizard we are done — phone will proceed to home screen
        setResult(RESULT_OK)
        finish()
    }
}