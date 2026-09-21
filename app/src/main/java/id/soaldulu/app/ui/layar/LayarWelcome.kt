package id.soaldulu.app.ui.layar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 1 — Welcome.
 *
 * Seluruh layar adalah area sentuh. Tidak ada tombol, sesuai berkas desain.
 */
@Composable
fun LayarWelcome(
    onLanjut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLanjut,
            )
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        TumpukanCatatan()

        Spacer(Modifier.height(48.dp))

        Text(
            "Welcome",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.weight(1f))

        Text(
            "Tap Anywhere to Continue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Dua lembar catatan bertumpuk dengan coretan.
 *
 * Digambar, bukan berkas gambar — tidak ada aset ilustrasi di proyek, dan
 * bentuk ini cukup sederhana untuk digambar langsung.
 */
@Composable
private fun TumpukanCatatan() {
    val kertas = Color(0xFFF3EDBE)
    val kertasBelakang = Color(0xFFE6DFA8)
    val tinta = Color(0xFF3A3A2E)

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 132.dp, height = 158.dp)
                .rotate(-9f)
                .shadow(6.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(kertasBelakang),
        )
        Box(
            Modifier
                .size(width = 132.dp, height = 158.dp)
                .rotate(6f)
                .shadow(8.dp, RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(kertas),
        ) {
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                val baris = 7
                val jarak = size.height / (baris + 1)
                repeat(baris) { i ->
                    val y = jarak * (i + 1)
                    val lebar = if (i == baris - 1) size.width * 0.55f else size.width
                    drawLine(
                        color = tinta.copy(alpha = 0.55f),
                        start = Offset(0f, y),
                        end = Offset(lebar, y),
                        strokeWidth = 2.5f,
                    )
                }
            }
        }
    }
}

@Preview(name = "Welcome", heightDp = 780)
@Composable
private fun PratinjauWelcome() {
    SoalduluTheme(paksaGelap = true) { LayarWelcome(onLanjut = {}) }
}
