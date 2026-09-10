package id.soaldulu.app.ui.layar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/** Layar 1 — Welcome (handoff Bagian 8 nomor 1). */
@Composable
fun LayarWelcome(
    onMulai: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Ditengahkan supaya layar tidak berat di atas. Tetap dapat digulir:
        // kalau ukuran font sistem diperbesar, kontennya melewati layar dan
        // penengahan otomatis menyerah ke penggulungan.
        verticalArrangement = Arrangement.Center,
    ) {
        LogoArch()

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("Soaldulu", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(8.dp))

        Text(
            "Tebus waktumu dengan soal.",
            style = Teks.caption,
            color = Accent,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Saat kamu membuka media sosial, Soaldulu menampilkan tiga soal " +
                "TKA/SNBT. Menjawabnya membuka akses selama beberapa menit. " +
                "Kredit habis, gerbang menutup lagi.",
            style = Teks.isi,
            color = OnBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        TombolPrimer("Mulai", onClick = onMulai)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Aplikasi penelitian · tidak dipublikasikan",
            style = Teks.caption,
            color = OnBackgroundDim,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

/**
 * Logo lengkung katedral dengan huruf S (handoff Bagian 7.6).
 *
 * Path SVG aslinya: M 0 120 L 0 45 A 50 45 0 0 1 100 45 L 100 120 Z
 * pada viewBox 100x120 — digambar ulang di Canvas supaya ikut menyesuaikan
 * kerapatan piksel layar.
 *
 * Bagian 7.6 menyebut elemen khas sebagai opsional dan dikerjakan terakhir,
 * tapi Bagian 8 nomor 1 menaruh logo ini sebagai elemen utama layar Welcome —
 * tanpa logo, layarnya kosong. Ini 20 baris dan mudah dibuang kalau kamu
 * ingin menundanya.
 */
@Composable
private fun LogoArch(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(width = 100.dp, height = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        val tebal = Ukuran.tebalGarisTombol
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Titik pangkal lengkung: 45/120 dari tinggi.
            val bahu = h * 45f / 120f
            val jalur = Path().apply {
                moveTo(0f, h)
                lineTo(0f, bahu)
                // Setengah elips atas: pusat (w/2, bahu), rx = w/2, ry = bahu.
                arcTo(
                    rect = Rect(0f, 0f, w, bahu * 2f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false,
                )
                lineTo(w, h)
                close()
            }
            drawPath(jalur, color = Accent, style = Stroke(width = tebal.toPx()))
        }
        Text("S", style = Teks.angkaKredit, color = Accent)
    }
}

@Preview(name = "Layar 1 — Welcome", heightDp = 800)
@Composable
private fun PratinjauWelcome() {
    SoalduluTheme { LayarWelcome(onMulai = {}) }
}
