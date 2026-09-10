package id.soaldulu.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── ISI BANK SOAL (hasil seeding dari bank_soal_v1.json) ────────────────────
//
// Nilai seperti subtest, exam, cognitiveLevel, dan source.type sengaja
// disimpan sebagai String, BUKAN enum Kotlin. Daftar subtes masih
// diverifikasi pengguna ke kisi-kisi resmi (handoff Bagian 10), dan enum
// yang keliru akan memaksa migrasi database di tengah uji coba.

@Entity(tableName = "paket")
data class PaketEntity(
    @PrimaryKey val packageId: String,
    val packageName: String,
    /** Dicatat di setiap baris log — penghubung data responden ke versi paket. */
    val version: String,
    val schemaVersion: Int,
    val releasedAt: String,
    val changelog: String,
    val itemCount: Int,
    val activeItemCount: Int,
    val diseedPada: Long,
)

@Entity(tableName = "bacaan")
data class BacaanEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Paragraf dipisah "\n\n". */
    val text: String,
    val sourceType: String,
    val sourceReference: String,
)

@Entity(
    tableName = "butir",
    indices = [Index("active"), Index("passageId")],
)
data class ButirEntity(
    @PrimaryKey val id: String,
    /**
     * active = false: butir tidak pernah ditampilkan, tapi TETAP tersimpan.
     * Menghapus butir cacat di tengah uji coba akan mengubah jumlah butir
     * dan merusak perbandingan antar-responden (handoff Bagian 5.2).
     */
    val active: Boolean,
    val exam: String,
    val subtest: String,
    /** null berarti soal berdiri sendiri tanpa bacaan. */
    val passageId: String?,
    val stem: String,
    val correctOptionId: String,
    val explanation: String,
    val cognitiveLevel: String,
    val estimatedSeconds: Int,
    val sourceType: String,
    val sourceReference: String,
)

@Entity(tableName = "opsi", indices = [Index("itemId")])
data class OpsiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    /** "A", "B", "C", "D" — sesuai options[].id di JSON. */
    val optionId: String,
    val text: String,
    val urutan: Int,
)

// ── LOG PENELITIAN (handoff Bagian 6) ───────────────────────────────────────
//
// Ini data KTI, bukan fitur tambahan.

@Entity(tableName = "log_jawaban", indices = [Index("gateSessionId"), Index("respondentCode")])
data class LogJawabanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val respondentCode: String,
    val packageVersion: String,
    /**
     * Penanda satu sesi gerbang, berisi epoch ms saat gerbang muncul.
     * Tambahan di luar Bagian 6.1: tanpa ini tiga jawaban dari gerbang yang
     * sama tidak bisa dikelompokkan, sehingga pola dalam satu gerbang
     * (misalnya ketelitian yang turun di soal terakhir) tidak bisa dianalisis.
     */
    val gateSessionId: Long,
    /** Soal ke berapa dalam gerbang ini, 1..GateConfig.QUESTIONS_PER_GATE. */
    val urutanDalamGerbang: Int,
    val itemId: String,
    val subtest: String,
    val selectedOptionId: String,
    val isCorrect: Boolean,
    val durationSeconds: Int,
    val creditEarnedSeconds: Int,
    /** Aplikasi yang memicu gerbang ini. */
    val triggeredByPackage: String,
    val timestamp: Long,
)

@Entity(tableName = "log_kredit", indices = [Index("respondentCode")])
data class LogKreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val respondentCode: String,
    val gateSessionId: Long,
    val creditGrantedSeconds: Int,
    val creditStartedAt: Long,
    /** null selama kredit masih berjalan. */
    val creditEndedAt: Long? = null,
    /** EXPIRED | MANUAL | SERVICE_KILLED. null selama masih berjalan. */
    val endReason: String? = null,
)

@Entity(tableName = "log_peristiwa", indices = [Index("respondentCode")])
data class LogPeristiwaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val respondentCode: String,
    /** PERMISSION_REVOKED | SERVICE_RESTARTED | BROWSER_OPENED | PACKAGE_UPDATED */
    val eventType: String,
    val detail: String,
    val timestamp: Long,
)
