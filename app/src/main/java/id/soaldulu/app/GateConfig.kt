package id.soaldulu.app

/**
 * Semua parameter perilaku aplikasi, dalam satu objek.
 *
 * Aturan keras (handoff Bagian 4): tidak boleh ada angka waktu ditulis
 * langsung di UI, service, atau repository. Selalu rujuk ke sini.
 * Semua durasi dalam DETIK.
 */
object GateConfig {

    // ── KURS KREDIT (nilai sementara, akan dikunci setelah uji coba mandiri) ──
    const val QUESTIONS_PER_GATE = 3
    const val GATE_REWARD_SECONDS = 600 // 10 menit, dasar
    const val CORRECT_BONUS_SECONDS = 180 // 3 menit per jawaban benar
    const val MAX_CREDIT_BALANCE_SECONDS = 3600 // 0 = tanpa batas

    // 3 soal, 0 benar → 600 detik  (10 menit)
    // 3 soal, 3 benar → 1140 detik (19 menit)

    // ── PERILAKU GERBANG ──
    const val FOREGROUND_POLL_INTERVAL_SECONDS = 1
    const val GATE_TRIGGER_DELAY_SECONDS = 2
    const val CREDIT_WARNING_BEFORE_EXPIRY_SECONDS = 60
    const val MIN_GENUINE_ANSWER_SECONDS = 2 // lebih cepat = tak dapat bonus
    const val GATE_COOLDOWN_SECONDS = 5

    // ── APLIKASI DIPANTAU ──
    // BELUM DIVERIFIKASI di HP responden (handoff Bagian 10). Daftar ini
    // disalin apa adanya dari handoff; nama package harus dicek ulang
    // sebelum APK dibagikan.
    val MONITORED_PACKAGES = setOf(
        "com.zhiliaoapp.musically", // TikTok
        "com.instagram.android",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.facebook.katana",
    )

    // Dicatat, TIDAK diblokir (handoff Bagian 3.6)
    val BROWSER_PACKAGES_FOR_LOGGING_ONLY = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
    )

    // ── PAKET SOAL ──
    const val BUNDLED_PACKAGE_ASSET = "bank_soal_v1.json"
    const val UPDATE_MANIFEST_URL = "" // kosong = fitur mati
    const val UPDATE_CHECK_INTERVAL_SECONDS = 86_400
    const val UPDATE_TIMEOUT_SECONDS = 15
    const val ACCEPT_PATCH_UPDATES_ONLY = true

    // ── PENELITIAN ──
    const val EXPORT_FILENAME_TEMPLATE = "soaldulu_log_%s.csv"
    const val DEV_MODE = false // WAJIB false di APK responden
}
