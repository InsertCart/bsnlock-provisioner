package com.bsnlock.provisioner.utils

object ProvisionerConfig {

    // ─── EmiLock target app ──────────────────────────────────────────────────
    // Used by: TransferActivity, OwnershipTransferManager, PermissionGranter
    const val EMILOCK_PACKAGE        = "com.bsnlock.app"
    const val EMILOCK_ADMIN_RECEIVER = "com.bsnlock.app.MyDeviceAdminReceiver"

    // ─── EmiLock APK download URL ────────────────────────────────────────────
    // Used by: TransferActivity "Download EmiLock" button
    const val DEFAULT_EMILOCK_APK_URL = "https://www.bsnlock.com/emilock.apk"

    // ─── Used by QRGeneratorActivity to build the QR code JSON ──────────────
    const val PROVISIONER_APK_URL   = "https://www.bsnlock.com/bsnlockprovisioner.apk"
    const val PROVISIONER_APK_CHECKSUM = "xNTTnB0pUlk1trCNOBnzlahq2MVItfffCNb0h8ZT3as" // No '=' padding
    const val PROVISIONER_ADMIN_COMPONENT =
        "com.bsnlock.provisioner/com.bsnlock.provisioner.receivers.ProvisionerAdminReceiver"

    const val QR_KEY_ADMIN_COMPONENT = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    const val QR_KEY_APK_URL         = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    const val QR_KEY_APK_CHECKSUM    = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
    const val QR_KEY_SKIP_ENCRYPTION = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    const val QR_KEY_LEAVE_APPS      = "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"
    const val QR_KEY_LOCALE          = "android.app.extra.PROVISIONING_LOCALE"
    const val QR_KEY_TIME_ZONE       = "android.app.extra.PROVISIONING_TIME_ZONE"

    const val DEFAULT_LOCALE    = "en_IN"
    const val DEFAULT_TIME_ZONE = "Asia/Kolkata"

    // ─── Extras key (ProvisionerAdminReceiver → ProvisioningCompleteActivity) ─
    const val KEY_EMILOCK_APK_URL = "emilock_apk_url"
}