package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.Kartu
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 3 — Persetujuan (handoff Bagian 8 nomor 3).
 *
 * Isinya harus jujur dan lengkap: ini yang membuat responden tahu persis
 * apa yang dicatat sebelum menyetujuinya. Jangan menambah baris "yang
 * dicatat" tanpa benar-benar mencatatnya, dan jangan mencatat apa pun yang
 * tidak tertulis di sini.
 */
@Composable
fun LayarPersetujuan(
    onSetuju: () -> Unit,
    onTidakBersedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text("LANGKAH 2 DARI 4", style = Teks.label, color = Accent)

        Spacer(Modifier.height(16.dp))

        Text("Persetujuan", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Kartu {
            Text("YANG DICATAT", style = Teks.label, color = Accent)
            Spacer(Modifier.height(12.dp))
            Butir("Nama aplikasi yang kamu buka dan berapa lama.")
            Butir("Jawaban soal — benar atau salah, dan berapa lama kamu menjawabnya.")
            Butir("Kredit waktu yang kamu peroleh dan kamu pakai.")
        }

        Spacer(Modifier.height(16.dp))

        Kartu {
            Text("YANG TIDAK DICATAT", style = Teks.label, color = OnBackgroundDim)
            Spacer(Modifier.height(12.dp))
            Butir("Isi chat, foto, kontak, atau lokasi.")
            Butir("Nama, email, atau nomor telepon.")
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Data tersimpan di HP-mu sendiri. Kamu boleh berhenti kapan saja " +
                "tanpa konsekuensi apa pun.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        TombolPrimer("Saya setuju", onClick = onSetuju)

        Spacer(Modifier.height(16.dp))

        Text(
            "Saya tidak bersedia",
            style = Teks.caption.copy(textDecoration = TextDecoration.Underline),
            color = OnBackgroundDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTidakBersedia)
                .padding(vertical = 8.dp),
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun Butir(teks: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("·", style = Teks.isi, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(teks, style = Teks.isi, color = OnBackground)
    }
}

@Preview(name = "Layar 3 — Persetujuan", heightDp = 900)
@Composable
private fun PratinjauPersetujuan() {
    SoalduluTheme { LayarPersetujuan(onSetuju = {}, onTidakBersedia = {}) }
}
