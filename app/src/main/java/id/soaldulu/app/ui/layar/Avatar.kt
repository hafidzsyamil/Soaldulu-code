package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.theme.Ukuran
import kotlinx.coroutines.launch

/**
 * Delapan warna avatar.
 *
 * Avatar bukan foto — tidak ada gambar yang diunggah dan tidak ada berkas
 * yang disimpan. Yang tersimpan hanya satu angka indeks.
 */
val WarnaAvatar = listOf(
    Color(0xFFBBE1FA),
    Color(0xFF3282B8),
    Color(0xFF0F4C75),
    Color(0xFF7FB77E),
    Color(0xFFD4BD3B),
    Color(0xFFE29578),
    Color(0xFFB39CD0),
    Color(0xFF8AA6A3),
)

/** Lingkaran avatar. `onEdit` non-null memunculkan lencana pensil. */
@Composable
fun Avatar(
    indeks: Int,
    ukuran: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val warna = WarnaAvatar[indeks.coerceIn(0, WarnaAvatar.lastIndex)]
    Box(modifier = modifier.size(ukuran), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(ukuran)
                .clip(CircleShape)
                .background(warna)
                .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Avatar",
                tint = Color(0xFF16222A),
                modifier = Modifier.size(ukuran * 0.62f),
            )
        }
        if (onEdit != null) {
            Box(
                Modifier
                    .size(ukuran * 0.3f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Ganti avatar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ukuran * 0.16f),
                )
            }
        }
    }
}

/**
 * Pemilih avatar Material 3.
 *
 * Memilih avatar langsung menyimpannya lewat `onPilih`, lalu lembarnya
 * turun dengan animasinya sendiri sebelum `onTutup` dipanggil. Kalau
 * lembar dilepas dari komposisi begitu saja, ia hilang seketika tanpa
 * animasi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PemilihAvatar(
    terpilih: Int,
    onPilih: (Int) -> Unit,
    onTutup: () -> Unit,
) {
    val keadaanLembar = rememberModalBottomSheetState()
    val lingkup = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = keadaanLembar,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Ukuran.marginLayar)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Pilih avatar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Hanya hiasan. Tidak ada foto yang diunggah.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(WarnaAvatar.indices.toList()) { i ->
                    Box(
                        Modifier
                            .size(Ukuran.avatarKecil + 16.dp)
                            .clip(CircleShape)
                            .then(
                                if (i == terpilih) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onPilih(i)
                                lingkup.launch { keadaanLembar.hide() }.invokeOnCompletion {
                                    if (!keadaanLembar.isVisible) onTutup()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Avatar(indeks = i, ukuran = Ukuran.avatarKecil)
                    }
                }
            }
        }
    }
}
