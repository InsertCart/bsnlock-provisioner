package com.emilock.provisioner.utils

object ProvisionerConfig {

    // ─── EmiLock target package ──────────────────────────────────────────────
    const val EMILOCK_PACKAGE        = "com.emilock.app"

    // BUG FIX #2: Was pointing to Provisioner's own receiver — now correctly points to EmiLock
    const val EMILOCK_ADMIN_RECEIVER = "com.emilock.app.MyDeviceAdminReceiver"

    // ─── EmiLock APK download ────────────────────────────────────────────────
    const val DEFAULT_EMILOCK_APK_URL = "https://www.bsnlock.com/emilock.apk"

    // Leave empty to skip verification, or set after building EmiLock release APK
    const val EMILOCK_APK_CHECKSUM    = ""

    // ─── Provisioner APK (hosted on server, referenced in QR code) ──────────
    const val PROVISIONER_APK_URL = "https://www.bsnlock.com/bsnlockprovisioner.apk"

    // BUG FIX #1: Removed the trailing '=' padding — Android Enterprise rejects padded checksums
    // Generate fresh with: keytool -printcert -jarfile app-release.apk → SHA256 hex → URL-safe Base64 no padding
    const val PROVISIONER_APK_CHECKSUM = "xNTTnB0pUlk1trCNOBnzlahq2MVItfffCNb0h8ZT3as"  // NO '=' at end

    // ─── Provisioner admin component (used in QR code JSON) ─────────────────
    // MUST be fully qualified — shorthand with leading dot does NOT work in QR provisioning
    const val PROVISIONER_ADMIN_COMPONENT =
        "com.emilock.provisioner/com.emilock.provisioner.receivers.ProvisionerAdminReceiver"

    // ─── Standard Android Enterprise QR JSON keys ───────────────────────────
    const val QR_KEY_ADMIN_COMPONENT = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    const val QR_KEY_APK_URL         = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    const val QR_KEY_APK_CHECKSUM    = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
    const val QR_KEY_SKIP_ENCRYPTION = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    const val QR_KEY_LEAVE_APPS      = "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"
    const val QR_KEY_LOCALE          = "android.app.extra.PROVISIONING_LOCALE"
    const val QR_KEY_TIME_ZONE       = "android.app.extra.PROVISIONING_TIME_ZONE"

    // ─── Custom extras passed through QR → Provisioner app ──────────────────
    const val KEY_EMILOCK_APK_URL = "emilock_apk_url"

    // ─── Locale / timezone defaults ─────────────────────────────────────────
    const val DEFAULT_LOCALE    = "en_IN"
    const val DEFAULT_TIME_ZONE = "Asia/Kolkata"
}