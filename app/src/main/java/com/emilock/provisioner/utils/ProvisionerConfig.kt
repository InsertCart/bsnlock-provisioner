package com.emilock.provisioner.utils

object ProvisionerConfig {

    // ─── EmiLock target package ─────────────────────────────────────────────
    const val EMILOCK_PACKAGE   = "com.emilock.app"
    const val EMILOCK_ADMIN_RECEIVER = "com.emilock.app.MyDeviceAdminReceiver"

    // ─── APK Download ────────────────────────────────────────────────────────
    // UPDATE THIS to your actual EmiLock APK download URL
    const val DEFAULT_EMILOCK_APK_URL = "https://your-server.com/emilock.apk"

    // APK SHA-256 checksum for verification (Base64-encoded)
    // Generate with: sha256sum emilock.apk | xxd -r -p | base64
    // UPDATE THIS after each EmiLock APK build
    const val EMILOCK_APK_CHECKSUM = ""   // Set this before production

    // ─── QR Code provisioning payload keys (Android Enterprise standard) ────
    // These are standard Android Enterprise JSON keys for the provisioning QR code
    const val QR_KEY_ADMIN_COMPONENT  = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    const val QR_KEY_APK_URL          = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    const val QR_KEY_APK_CHECKSUM     = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
    const val QR_KEY_SKIP_ENCRYPTION  = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    const val QR_KEY_LOCALE           = "android.app.extra.PROVISIONING_LOCALE"
    const val QR_KEY_TIME_ZONE        = "android.app.extra.PROVISIONING_TIME_ZONE"
    const val QR_KEY_WIFI_SSID        = "android.app.extra.PROVISIONING_WIFI_SSID"
    const val QR_KEY_WIFI_PASSWORD    = "android.app.extra.PROVISIONING_WIFI_PASSWORD"
    const val QR_KEY_WIFI_SECURITY    = "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"
    const val QR_KEY_ADMIN_EXTRAS     = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"

    // Custom extras passed through provisioning to our app
    const val KEY_EMILOCK_APK_URL     = "emilock_apk_url"

    // ─── Provisioner APK download URL (for the QR code) ─────────────────────
    // This is where the PROVISIONER app itself is hosted — Android downloads this during setup wizard
    // UPDATE THIS to your actual provisioner APK download URL
    const val PROVISIONER_APK_URL      = "https://your-server.com/emilock-provisioner.apk"

    // Provisioner APK checksum (SHA-256, Base64 encoded, URL-safe)
    // UPDATE THIS after each Provisioner APK build
    const val PROVISIONER_APK_CHECKSUM = ""  // Set this before production

    // ─── Provisioner admin component (for QR code) ──────────────────────────
    const val PROVISIONER_ADMIN_COMPONENT = "com.emilock.provisioner/.receivers.ProvisionerAdminReceiver"

    // ─── Default locale/timezone ─────────────────────────────────────────────
    const val DEFAULT_LOCALE    = "en_IN"
    const val DEFAULT_TIME_ZONE = "Asia/Kolkata"
}