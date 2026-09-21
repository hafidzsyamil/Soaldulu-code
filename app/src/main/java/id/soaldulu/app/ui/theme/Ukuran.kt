package id.soaldulu.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Jarak. Bentuk sudut memakai MaterialTheme.shapes bawaan M3, tidak
 * didefinisikan ulang di sini.
 *
 * Kanvas mockup bukan ukuran sebenarnya. Jangan menyalin angka piksel dari
 * berkas desain — semua layar harus dapat digulir dan tidak memakai posisi
 * absolut.
 */
object Ukuran {
    /** Margin kiri-kanan setiap layar. */
    val marginLayar = 20.dp

    /** Jarak antar bagian besar. */
    val antarBagian = 28.dp

    val paddingKartu = 16.dp

    /** Tinggi minimum satu baris pilihan jawaban. */
    val tinggiBarisPilihan = 56.dp

    /** Diameter avatar di Settings dan layar nama. */
    val avatarBesar = 120.dp
    val avatarKecil = 44.dp

    val jarakKartu = 12.dp
}
