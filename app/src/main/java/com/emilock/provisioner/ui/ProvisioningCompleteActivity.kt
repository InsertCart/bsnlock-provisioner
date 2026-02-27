package com.emilock.provisioner.ui

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.emilock.provisioner.R
import com.emilock.provisioner.receivers.ProvisionerAdminReceiver
import com.emilock.provisioner.utils.ApkInstaller
import com.emilock.provisioner.utils.OwnershipTransferManager
import com.emilock.provisioner.utils.PermissionGranter
import com.emilock.provisioner.utils.ProvisionerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ProvisioningCompleteActivity
 *
 * This is shown DURING the Android Setup Wizard, after the device is provisioned as
 * a Managed Device. The setup wizard stays running in the background.
 *
 * This activity:
 *  1. Shows "Setting up device protection..." to the user
 *  2. Downloads EmiLock APK silently
 *  3. Installs EmiLock silently (no user prompt, Device Owner privilege)
 *  4. Grants all permissions to EmiLock silently
 *  5. Transfers Device Owner to EmiLock
 *  6. Calls setResult(RESULT_OK) so Setup Wizard knows provisioning is done
 *  7. Finishes → Android Setup Wizard continues to the home screen
 */
class ProvisioningCompleteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmiLock.SetupActivity"
    }

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnRetry: Button

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getIntExtra(
                android.content.pm.PackageInstaller.EXTRA_STATUS, -1
            )
            Log.d(TAG, "Install result received: status=$status")
            when (status) {
                android.content.pm.PackageInstaller.STATUS_SUCCESS -> {
                    log("✅ EmiLock installed successfully")
                    onEmiLockInstalled()
                }
                android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    // Fallback: user must confirm install
                    val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    confirmIntent?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                else -> {
                    val msg = intent?.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE)
                    log("❌ Install failed: $msg")
                    showRetry("Installation failed. Tap Retry.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_complete)

        dpm   = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ProvisionerAdminReceiver.getComponentName(this)

        tvStatus    = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        tvProgress  = findViewById(R.id.tvProgress)
        btnRetry    = findViewById(R.id.btnRetry)
        btnRetry.visibility = View.GONE

        btnRetry.setOnClickListener {
            btnRetry.visibility = View.GONE
            startSetupFlow()
        }

        // Register install result receiver
        val filter = IntentFilter("com.emilock.provisioner.INSTALL_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(installReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(installReceiver, filter)
        }

        // Check if EmiLock is already installed (e.g. app was reinstalled/updated)
        if (isEmiLockInstalled()) {
            log("EmiLock already installed — skipping download")
            onEmiLockInstalled()
        } else {
            startSetupFlow()
        }
    }

    private fun startSetupFlow() {
        val apkUrl = intent.getStringExtra(ProvisionerConfig.KEY_EMILOCK_APK_URL)
            ?: ProvisionerConfig.DEFAULT_EMILOCK_APK_URL

        lifecycleScope.launch {
            try {
                // ── Phase 1: Download ────────────────────────────────────────
                log("📥 Downloading EmiLock…")
                progressBar.isIndeterminate = false

                val apkFile = ApkInstaller.downloadApk(this@ProvisioningCompleteActivity, apkUrl) { progress ->
                    runOnUiThread {
                        progressBar.progress = progress
                        tvProgress.text = "Downloading… $progress%"
                    }
                }

                if (apkFile == null) {
                    showRetry("Download failed. Check internet connection.")
                    return@launch
                }

                // ── Phase 2: Verify checksum ────────────────────────────────
                log("🔍 Verifying package…")
                if (!ApkInstaller.verifyChecksum(apkFile, ProvisionerConfig.EMILOCK_APK_CHECKSUM)) {
                    showRetry("Package verification failed.")
                    return@launch
                }

                // ── Phase 3: Install silently ───────────────────────────────
                log("📦 Installing EmiLock…")
                progressBar.isIndeterminate = true
                val installed = ApkInstaller.installSilently(this@ProvisioningCompleteActivity, apkFile)
                if (!installed) {
                    // Fallback to dialog install
                    log("Falling back to dialog install…")
                    ApkInstaller.installWithDialog(this@ProvisioningCompleteActivity, apkFile)
                }
                // Wait for installReceiver to fire

            } catch (e: Exception) {
                Log.e(TAG, "Setup flow error: ${e.message}")
                showRetry("Error: ${e.message}")
            }
        }
    }

    /**
     * Called after EmiLock is confirmed installed.
     * Grant permissions + transfer Device Owner.
     */
    private fun onEmiLockInstalled() {
        lifecycleScope.launch {
            // ── Phase 4: Grant all permissions to EmiLock silently ───────────
            log("🔐 Granting permissions to EmiLock…")
            PermissionGranter.grantAllToPackage(
                dpm, admin, this@ProvisioningCompleteActivity,
                ProvisionerConfig.EMILOCK_PACKAGE
            )

            delay(1000)

            // ── Phase 5: Launch EmiLock so it can activate its DeviceAdminReceiver ──
            log("🚀 Activating EmiLock admin…")
            launchEmiLockForAdminActivation()
            delay(3000) // Give EmiLock time to activate its admin

            // ── Phase 6: Transfer Device Owner ───────────────────────────────
            log("🔄 Transferring Device Owner to EmiLock…")
            val transferred = OwnershipTransferManager.transferToEmiLock(
                this@ProvisioningCompleteActivity, dpm
            )

            if (transferred) {
                log("✅ Setup complete! EmiLock is now Device Owner.")
                delay(1500)
                finishProvisioning()
            } else {
                // Transfer failed — EmiLock admin might not be active yet
                log("⚠️ Transfer failed — EmiLock must activate its admin first.")
                showRetry("Tap Retry after EmiLock admin is activated.")
            }
        }
    }

    /**
     * Launch EmiLock so it can register its DeviceAdminReceiver.
     * EmiLock's MainActivity will auto-activate its admin on first launch.
     */
    private fun launchEmiLockForAdminActivation() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(
                ProvisionerConfig.EMILOCK_PACKAGE
            )?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("from_provisioner", true)
            }
            launchIntent?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch EmiLock: ${e.message}")
        }
    }

    /**
     * Signal to the Android Setup Wizard that our DPC setup is done.
     * The wizard will then proceed to the home screen.
     */
    private fun finishProvisioning() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        try { unregisterReceiver(installReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            tvStatus.text = message
        }
    }

    private fun showRetry(message: String) {
        runOnUiThread {
            tvStatus.text = message
            btnRetry.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }

    private fun isEmiLockInstalled(): Boolean = try {
        packageManager.getPackageInfo(ProvisionerConfig.EMILOCK_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}