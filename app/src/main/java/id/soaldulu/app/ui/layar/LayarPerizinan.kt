package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.StatusIzin
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 2 — Permission.
 *
 * Menyentuh kartu meminta izin yang bersangkutan. Menyentuh layar melanjutkan,
 * tapi hanya setelah keempatnya aktif: melewatinya membuat gerbang tidak
 * pernah muncul sama sekali, dan data responden itu hilang tanpa ada yang tahu.
 */
@Composable
fun LayarPerizinan(
    status: StatusIzin,
    onMinta: (Int) -> Unit,
    onLanjut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baris = listOf(
        Triple(1, "Akses Penggunaan", status.usageAccess),
        Triple(2, "Tampil di Atas Aplikasi Lain", status.overlay),
        Triple(3, "Notifikasi", status.notifikasi),
        Triple(4, "Abaikan Optimasi Baterai", status.baterai),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = status.semuaAktif,
                onClick = onLanjut,
            )
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Permission",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Column(verticalArrangement = Arrangement.spacedBy(Ukuran.jarakKartu)) {
            baris.forEach { (nomor, nama, aktif) ->
                KartuIzin(nama = nama, aktif = aktif, onKlik = { onMinta(nomor) })
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            if (status.semuaAktif) {
                "Tap Anywhere to Continue"
            } else {
                "${status.jumlahAktif} dari 4 aktif · sentuh kartu untuk memberi izin"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KartuIzin(nama: String, aktif: Boolean, onKlik: () -> Unit) {
    Card(
        onClick = onKlik,
        enabled = !aktif,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Ukuran.paddingKartu, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                nama,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (aktif) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Sudah aktif",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Preview(name = "Permission", heightDp = 780)
@Composable
private fun PratinjauPerizinan() {
    SoalduluTheme(paksaGelap = true) {
        LayarPerizinan(
            status = StatusIzin(
                usageAccess = true,
                overlay = true,
                notifikasi = false,
                baterai = true,
            ),
            onMinta = {},
            onLanjut = {},
        )
    }
}
