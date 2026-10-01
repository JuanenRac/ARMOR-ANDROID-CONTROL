// ARMOR-ANDROID-CONTROL - the "find nodes on the network" panel shared by the Solar, Electrical and Radar screens. Mirrors ARMOR-STUDIO's NodeFinder component.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.electrohobby3d.armor.model.NetworkOverview
import es.electrohobby3d.armor.model.NodeCandidate
import es.electrohobby3d.armor.model.NodeFinder

/** Lists what the network node has found that looks like an ARMOR node and is not yet known here, with a way to search now and to open a candidate's own panel. */
@Composable
fun NodeFinderPanel(network: NetworkOverview?, knownIds: Collection<String>, onScan: () -> Unit, scanning: Boolean) {
    val candidates = NodeFinder.candidates(network, knownIds)
    val context = LocalContext.current
    val hasWatcher = network?.nodes?.any { !it.stale } == true
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Buscar nodos en la red", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Button(onClick = onScan, enabled = hasWatcher && !scanning) { Text(if (scanning) "Buscando…" else "Buscar ahora") }
            }
            if (!hasWatcher) Text("El nodo de red no informa, así que no hay con qué buscar.", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            else if (candidates.isEmpty()) Text("No se ha encontrado ningún nodo nuevo.", style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
            candidates.forEach { candidate -> CandidateRow(candidate, context) }
        }
    }
}

@Composable
private fun CandidateRow(candidate: NodeCandidate, context: android.content.Context) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(candidate.nodeId ?: candidate.hostname ?: candidate.ip, fontWeight = FontWeight.Medium)
            Text(
                listOfNotNull(candidate.ip, candidate.mac, candidate.vendor, if (!candidate.online) "no responde ahora" else null).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted,
            )
        }
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://${candidate.ip}/"))) }) { Text("Panel") }
    }
}
