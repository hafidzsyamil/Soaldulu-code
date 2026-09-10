package id.soaldulu.app

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/** Status keempat izin yang dibutuhkan (handoff Bagian 3.3). */
data class StatusIzin(
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val notifikasi: Boolean = false,
    val baterai: Boolean = false,
) {
    val jumlahAktif: Int
        get() = listOf(usageAccess, overlay, notifikasi, baterai).count { it }

    val semuaAktif: Boolean get() = jumlahAktif == 4
}

fun bacaStatusIzin(context: Context) = StatusIzin(
    usageAccess = punyaUsageAccess(context),
    overlay = Settings.canDrawOverlays(context),
    notifikasi = punyaNotifikasi(context),
    baterai = punyaPengecualianBaterai(context),
)

/**
 * Akses Penggunaan tidak muncul di daftar izin biasa; satu-satunya cara
 * mengeceknya adalah lewat AppOpsManager.
 */
private fun punyaUsageAccess(context: Context): Boolean {
    val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = aom.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun punyaNotifikasi(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true // sebelum Android 13 notifikasi tidak butuh izin runtime
    }

private fun punyaPengecualianBaterai(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// ── Jalur ke Settings ───────────────────────────────────────────────────────
// Nama menu di One UI Samsung belum diverifikasi. Teks penuntun untuk Layar 4
// baru ditulis setelah pengguna mengecek jalur persisnya di HP (handoff 3.3).

fun bukaSettingsUsageAccess(context: Context) {
    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}

fun bukaSettingsOverlay(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    )
}

fun bukaSettingsBaterai(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        )
    } catch (e: ActivityNotFoundException) {
        // Sebagian OEM tidak menyediakan dialog langsungnya. Jatuh ke daftar
        // umum, lalu pengguna memilih Soaldulu sendiri.
        SpikeLog.tulis(context, "BATERAI dialog langsung tidak tersedia: ${e.message}")
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
