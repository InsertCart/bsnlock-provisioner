package com.bsnlock.provisioner.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bsnlock.provisioner.R
import com.bsnlock.provisioner.receivers.ProvisionerAdminReceiver
import com.bsnlock.provisioner.utils.OwnershipTransferManager
import com.bsnlock.provisioner.utils.PermissionGranter
import com.bsnlock.provisioner.utils.ProvisionerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TransferActivity — Dealer-facing screen.
 *
 * Dealer flow:
 *  1. Factory reset phone → scan QR → Provisioner becomes Device Owner
 *  2. Phone boots normally to home screen
 *  3. Dealer downloads + installs EmiLock APK manually (browser / USB)
 *  4. Dealer opens Provisioner app → this screen appears
 *  5. Screen shows status: is Device Owner? is EmiLock installed?
 *  6. Dealer taps "Transfer to EmiLock" button
 *  7. Provisioner grants all permissions → activates EmiLock admin → transfers DO
 *  8. Done — EmiLock is Device Owner, dealer hands phone to customer
 */
class TransferActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmiLock.Transfer"
        private const val ADMIN_POLL_INTERVAL_MS  = 500L
        private const val ADMIN_POLL_MAX_ATTEMPTS = 20  // 10 seconds
    }

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    // Views
    private lateinit var ivStatusDO: ImageView
    private lateinit var ivStatusEmiLock: ImageView
    private lateinit var tvStatusDO: TextView
    private lateinit var tvStatusEmiLock: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnTransfer: Button
    private lateinit var btnInstallEmiLock: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutReady: LinearLayout
    private lateinit var layoutDone: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        dpm   = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ProvisionerAdminReceiver.getComponentName(this)

        bindViews()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time dealer comes back (e.g. after installing EmiLock)
        refreshStatus()
    }

    private fun bindViews() {
        ivStatusDO       = findViewById(R.id.ivStatusDO)
        ivStatusEmiLock  = findViewById(R.id.ivStatusEmiLock)
        tvStatusDO       = findViewById(R.id.tvStatusDO)
        tvStatusEmiLock  = findViewById(R.id.tvStatusEmiLock)
        tvLog            = findViewById(R.id.tvLog)
        btnTransfer      = findViewById(R.id.btnTransfer)
        btnInstallEmiLock = findViewById(R.id.btnInstallEmiLock)
        progressBar      = findViewById(R.id.progressBar)
        layoutReady      = findViewById(R.id.layoutReady)
        layoutDone       = findViewById(R.id.layoutDone)

        progressBar.visibility = View.GONE
        layoutDone.visibility  = View.GONE

        btnTransfer.setOnClickListener { startTransfer() }

        btnInstallEmiLock.setOnClickListener {
            // Open browser to download EmiLock APK
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(ProvisionerConfig.DEFAULT_EMILOCK_APK_URL)
            }
            startActivity(intent)
        }
    }

    private fun refreshStatus() {
        val isDeviceOwner   = dpm.isDeviceOwnerApp(packageName)
        val isEmiLockReady  = isEmiLockInstalled()

        // Device Owner status
        if (isDeviceOwner) {
            ivStatusDO.setImageResource(android.R.drawable.presence_online)
            tvStatusDO.text = "✅ This device is provisioned (Device Owner active)"
        } else {
            ivStatusDO.setImageResource(android.R.drawable.presence_offline)
            tvStatusDO.text = "❌ Not Device Owner — please factory reset and scan QR first"
        }

        // EmiLock installation status
        if (isEmiLockReady) {
            ivStatusEmiLock.setImageResource(android.R.drawable.presence_online)
            tvStatusEmiLock.text = "✅ EmiLock app is installed and ready"
            btnInstallEmiLock.visibility = View.GONE
        } else {
            ivStatusEmiLock.setImageResource(android.R.drawable.presence_offline)
            tvStatusEmiLock.text = "❌ EmiLock not installed — tap button below to download"
            btnInstallEmiLock.visibility = View.VISIBLE
        }

        // Enable transfer button only when both conditions are met
        btnTransfer.isEnabled = isDeviceOwner && isEmiLockReady
        btnTransfer.alpha     = if (btnTransfer.isEnabled) 1.0f else 0.4f
    }

    private fun startTransfer() {
        btnTransfer.isEnabled  = false
        progressBar.visibility = View.VISIBLE
        layoutReady.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Step 1: Grant all permissions to EmiLock silently (we are still DO)
                log("🔐 Granting permissions to EmiLock…")
                PermissionGranter.grantAllToPackage(
                    dpm, admin, this@TransferActivity,
                    ProvisionerConfig.EMILOCK_PACKAGE
                )
                delay(300)

                // Step 2: Launch EmiLock with flag so it activates its DeviceAdminReceiver
                log("🚀 Activating EmiLock admin…")
                launchEmiLockForAdminActivation()

                // Step 3: Poll until EmiLock's admin is active (up to 10 seconds)
                val emiLockAdmin = ComponentName(
                    ProvisionerConfig.EMILOCK_PACKAGE,
                    ProvisionerConfig.EMILOCK_ADMIN_RECEIVER
                )
                var adminActive = false
                repeat(ADMIN_POLL_MAX_ATTEMPTS) { attempt ->
                    if (!adminActive) {
                        delay(ADMIN_POLL_INTERVAL_MS)
                        adminActive = try {
                            dpm.isAdminActive(emiLockAdmin)
                        } catch (_: Exception) { false }
                        log("⏳ Waiting for EmiLock admin… (${attempt + 1}/$ADMIN_POLL_MAX_ATTEMPTS) active=$adminActive")
                    }
                }

                if (!adminActive) {
                    log("⚠️ EmiLock admin not detected — attempting transfer anyway…")
                }

                // Step 4: Transfer Device Owner from Provisioner → EmiLock
                log("🔄 Transferring Device Owner to EmiLock…")
                val transferred = OwnershipTransferManager.transferToEmiLock(
                    this@TransferActivity, dpm
                )

                progressBar.visibility = View.GONE

                if (transferred) {
                    log("✅ Done! EmiLock is now Device Owner.")
                    showSuccess()
                } else {
                    log("❌ Transfer failed. Is EmiLock installed and launched at least once?")
                    showRetry()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Transfer error: ${e.message}")
                log("❌ Error: ${e.message}")
                progressBar.visibility = View.GONE
                showRetry()
            }
        }
    }

    private fun launchEmiLockForAdminActivation() {
        try {
            val launchIntent = packageManager
                .getLaunchIntentForPackage(ProvisionerConfig.EMILOCK_PACKAGE)
                ?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("from_provisioner", true)
                }
            launchIntent?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch EmiLock: ${e.message}")
        }
    }

    private fun showSuccess() {
        runOnUiThread {
            layoutDone.visibility  = View.VISIBLE
            layoutReady.visibility = View.GONE
        }
    }

    private fun showRetry() {
        runOnUiThread {
            layoutReady.visibility = View.VISIBLE
            btnTransfer.isEnabled  = true
            btnTransfer.alpha      = 1.0f
            btnTransfer.text       = "RETRY TRANSFER"
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            tvLog.text = "${tvLog.text}\n$message".trimStart('\n')
        }
    }

    private fun isEmiLockInstalled(): Boolean = try {
        packageManager.getPackageInfo(ProvisionerConfig.EMILOCK_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}