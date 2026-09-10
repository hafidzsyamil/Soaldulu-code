package id.soaldulu.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Jarak dan bentuk (handoff Bagian 7.4).
 *
 * Kanvas mockup 1080x1920 px. JANGAN menyalin angka px dari mockup ke kode —
 * pakai nilai dp di sini. Perangkat nyata lebih tinggi dari 16:9, jadi semua
 * layar harus dapat digulir dan tidak boleh memakai posisi absolut.
 */
object Ukuran {

    /** Margin kiri-kanan setiap layar. */
    val marginLayar = 24.dp

    /** Jarak antar bagian besar dalam satu layar. */
    val antarBagian = 32.dp

    /** Padding di dalam kartu dan panel. */
    val paddingKartu = 16.dp

    val tinggiTombol = 56.dp

    /** Tinggi minimum satu baris pilihan jawaban — batas nyaman untuk jempol. */
    val tinggiBarisPilihan = 44.dp

    val tinggiKolomIsian = 56.dp

    /** Diameter lencana huruf opsi (A/B/C/D). */
    val lencanaOpsi = 24.dp

    /** Tebal strip kilau di tepi atas tombol primer. */
    val stripKilau = 2.dp

    val tebalGaris = 1.dp
    val tebalGarisTombol = 2.dp
}

object Bentuk {
    /** Kartu, panel, kolom isian. */
    val kartu = RoundedCornerShape(4.dp)

    val tombol = RoundedCornerShape(8.dp)
}
