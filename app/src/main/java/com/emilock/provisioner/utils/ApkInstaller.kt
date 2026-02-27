package com.emilock.provisioner.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Base64

/**
 * ApkInstaller
 *
 * Downloads EmiLock APK from the server and installs it silently using
 * PackageInstaller (available to Device Owner apps with no user prompt).
 *
 * Silent install via PackageInstaller.Session is the proper DO approach.
 * No "Install unknown apps" permission needed — Device Owner can install silently.
 */
object ApkInstaller {

    private const val TAG      = "EmiLock.Installer"
    private const val APK_NAME = "emilock.apk"

    /**
     * Download EmiLock APK from URL.
     * Reports progress via callback (0–100).
     *
     * @return the downloaded File, or null on failure
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val destFile = File(context.cacheDir, APK_NAME)

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout    = 60_000
            connection.connect()

            val fileLength = connection.contentLength
            val input  = connection.inputStream
            val output = FileOutputStream(destFile)

            val buffer = ByteArray(4096)
            var downloaded = 0L
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                downloaded += read
                if (fileLength > 0) {
                    onProgress(((downloaded * 100) / fileLength).toInt())
                }
            }

            output.flush()
            output.close()
            input.close()
            connection.disconnect()

            Log.d(TAG, "APK downloaded to ${destFile.absolutePath} (${destFile.length()} bytes)")
            destFile

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            null
        }
    }

    /**
     * Verify the downloaded APK's SHA-256 checksum.
     * Pass an empty checksum to skip verification.
     */
    fun verifyChecksum(file: File, expectedBase64: String): Boolean {
        if (expectedBase64.isEmpty()) {
            Log.w(TAG, "Checksum verification skipped (no expected checksum set)")
            return true
        }
        return try {
            val digest  = MessageDigest.getInstance("SHA-256")
            val bytes   = file.readBytes()
            val hash    = digest.digest(bytes)
            val actual  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
            } else {
                android.util.Base64.encodeToString(hash, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING).trim()
            }
            val matches = actual == expectedBase64.trim()
            if (!matches) Log.e(TAG, "Checksum mismatch! expected=$expectedBase64, actual=$actual")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Checksum verification error: ${e.message}")
            false
        }
    }

    /**
     * Install APK silently using PackageInstaller (Device Owner privilege).
     *
     * This requires NO user interaction — the system silently installs.
     * Only works when this app is the Device Owner.
     *
     * @return true if installation session was committed successfully
     */
    fun installSilently(context: Context, apkFile: File): Boolean {
        return try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(ProvisionerConfig.EMILOCK_PACKAGE)
            }

            val sessionId = packageInstaller.createSession(params)
            val session   = packageInstaller.openSession(sessionId)

            // Write APK to session
            apkFile.inputStream().use { apkStream ->
                session.openWrite("package", 0, apkFile.length()).use { sessionStream ->
                    apkStream.copyTo(sessionStream)
                    session.fsync(sessionStream)
                }
            }

            // Create a PendingIntent for install result callback
            val installIntent = Intent("com.emilock.provisioner.INSTALL_COMPLETE").apply {
                setPackage(context.packageName)
            }
            val pendingFlags  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, sessionId, installIntent, pendingFlags
            )

            session.commit(pendingIntent.intentSender)
            session.close()

            Log.d(TAG, "✅ Silent install session committed for ${ProvisionerConfig.EMILOCK_PACKAGE}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Silent install failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Fallback: Show system install dialog if silent install fails.
     * This prompts the user but requires no permissions.
     */
    fun installWithDialog(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}