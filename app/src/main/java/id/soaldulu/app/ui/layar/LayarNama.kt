package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran
import id.soaldulu.app.ui.theme.WarnaTambah

/**
 * Layar 3 — Enter your Name.
 *
 * Menekan tombol centang berarti menyetujui syarat penggunaan. Persetujuan
 * tidak lagi berupa layar tersendiri; teksnya ada di layar Terms and
 * Condition yang bisa dibuka dari sini.
 */
@Composable
fun LayarNama(
    namaAwal: String,
    avatar: Int,
    onGantiAvatar: () -> Unit,
    onSelesai: (String) -> Unit,
    onBukaSyarat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nama by remember { mutableStateOf(namaAwal) }
    val bersih = nama.trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        Avatar(indeks = avatar, ukuran = Ukuran.avatarBesar, onEdit = onGantiAvatar)

        Spacer(Modifier.height(32.dp))

        Text(
            "Enter your Name",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nama,
            onValueChange = { nama = it },
            singleLine = true,
            label = { Text("Nama") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = { onSelesai(bersih) },
                    enabled = bersih.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Simpan dan lanjut")
                }
            },
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = buildAnnotatedString {
                append("clicking the check button mean agree to ")
                withStyle(
                    SpanStyle(
                        color = WarnaTambah.tautan,
                        textDecoration = TextDecoration.Underline,
                    )
                ) {
                    append("terms and condition")
                }
                append(" for you data usage")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                // Seluruh kalimat dapat disentuh, bukan hanya kata bertautan:
                // area sentuh selebar dua kata terlalu kecil untuk jempol.
                .clickable(onClick = onBukaSyarat)
                .padding(vertical = 12.dp),
        )
    }
}

@Preview(name = "Enter your Name", heightDp = 780)
@Composable
private fun PratinjauNama() {
    SoalduluTheme(paksaGelap = true) {
        LayarNama(
            namaAwal = "",
            avatar = 0,
            onGantiAvatar = {},
            onSelesai = {},
            onBukaSyarat = {},
        )
    }
}
