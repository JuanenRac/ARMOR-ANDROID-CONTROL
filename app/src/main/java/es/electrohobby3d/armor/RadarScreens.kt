// ARMOR-ANDROID-CONTROL - the Radar screen: the site as Studio designed it, in 2D and in 3D, with the targets of the radars moving on it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.electrohobby3d.armor.model.FieldNode
import es.electrohobby3d.armor.model.PlacedTarget
import es.electrohobby3d.armor.model.RadarGeometry
import es.electrohobby3d.armor.model.SiteBuilding
import es.electrohobby3d.armor.model.SiteDesign
import es.electrohobby3d.armor.model.SiteFeature
import es.electrohobby3d.armor.model.SitePoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

private val GroundColour = Color(0xFF17414D)
private val WallColour = Color(0xFFD9E3E6)
private val RoofColour = Color(0xFF9A4D3F)
private val TargetColour = Color(0xFFFFB020)
private val IgnoredColour = Color(0xFF7D8B93)

private fun hex(text: String?, fallback: Color): Color = runCatching { Color(android.graphics.Color.parseColor(text)) }.getOrDefault(fallback)
private fun Color.shade(factor: Float) = Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)

@Composable
fun RadarScreen(design: SiteDesign?, loaded: Boolean, nodes: List<FieldNode>, onReload: () -> Unit) {
    var threeD by rememberSaveable { mutableStateOf(false) }
    val (targets, unplaced) = remember(design, nodes) { if (design == null) emptyList<PlacedTarget>() to 0 else RadarGeometry.place(design, nodes) }
    val counted = targets.count { it.counted }
    val online = nodes.count { it.online && !it.stale }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(Icons.Filled.Radar, "Radar", if (design?.hasRadar == true) "En directo" else null) {
            IconButton(onClick = { threeD = false }) { Icon(Icons.Filled.Map, contentDescription = "Vista 2D", tint = if (!threeD) ArmorColors.Cyan else ArmorColors.Muted) }
            IconButton(onClick = { threeD = true }) { Icon(Icons.Filled.ViewInAr, contentDescription = "Vista 3D", tint = if (threeD) ArmorColors.Cyan else ArmorColors.Muted) }
            IconButton(onClick = onReload) { Icon(Icons.Filled.Refresh, contentDescription = "Recargar el diseño", tint = ArmorColors.Muted) }
        }
        when {
            !loaded -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            design == null || !design.hasRadar -> Panel(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.Radar, ArmorColors.Muted, size = 64.dp)
                    Text("Aún no hay un radar diseñado", style = MaterialTheme.typography.titleMedium)
                    Text("Coloca tus radares en el Diseñador de sitio de Studio y aparecerán aquí, con las personas que detectan.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Muted, textAlign = TextAlign.Center)
                }
            }
            else -> {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF060D11))) {
                    if (threeD) Site3D(design, targets, Modifier.fillMaxSize()) else Site2D(design, targets, Modifier.fillMaxSize())
                    Row(Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(Icons.AutoMirrored.Filled.DirectionsWalk, if (counted == 1) "1 persona" else "$counted personas", if (counted > 0) TargetColour else ArmorColors.Muted)
                        Chip(Icons.Filled.Sensors, "$online/${nodes.size} nodos", if (nodes.isNotEmpty() && online == nodes.size) ArmorColors.Ok else ArmorColors.Amber)
                    }
                    Text(if (threeD) "Arrastra para girar · pellizca para acercar" else "Arrastra y pellizca · doble toque para centrar", Modifier.align(Alignment.BottomCenter).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = ArmorColors.Muted)
                }
                if (unplaced > 0) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = ArmorColors.Amber, modifier = Modifier.size(18.dp))
                    Text("Hay detecciones de un nodo que no está enlazado a un radar del diseño.", style = MaterialTheme.typography.bodySmall, color = ArmorColors.Amber)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(
        Modifier.background(Color(0xCC0B1218), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)); Text(text, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
}

// ---- 2D --------------------------------------------------------------------------------------------------------------------------

private fun rectCorners(f: SiteFeature): List<SitePoint> {
    val r = f.rotation * PI / 180.0
    val c = cos(r)
    val s = sin(r)
    return listOf(-1 to -1, 1 to -1, 1 to 1, -1 to 1).map { (sx, sy) ->
        val lx = sx * f.width / 2
        val ly = sy * f.depth / 2
        SitePoint(f.x + lx * c - ly * s, f.y + lx * s + ly * c)
    }
}

/** The part of the plane that holds the site: its ground, buildings and the field of every radar and camera - not the empty space around the origin. */
private class SiteBounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

private fun siteBounds(design: SiteDesign): SiteBounds {
    val dims = design.dimensions
    val points = mutableListOf<SitePoint>()
    points += design.terrain
    design.buildings.forEach { points += it.points }
    design.sensors.forEach { s ->
        val apex = RadarGeometry.toMetres(s.x, s.y, dims)
        val (range, half) = RadarGeometry.view(s.kind)
        points += RadarGeometry.sector(apex, RadarGeometry.headingOf(s.x, s.y, s.heading, dims), half, range)
    }
    if (points.isEmpty()) points += listOf(SitePoint(0.0, 0.0), SitePoint(dims.width, dims.depth))
    return SiteBounds(points.minOf { it.x }, points.maxOf { it.x }, points.minOf { it.y }, points.maxOf { it.y })
}

@Composable
private fun Site2D(design: SiteDesign, targets: List<PlacedTarget>, modifier: Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val measurer = rememberTextMeasurer()
    val pulse = rememberPulse()
    Canvas(
        modifier
            .pointerInput(Unit) { detectTransformGestures { _, change, zoomChange, _ -> zoom = (zoom * zoomChange).coerceIn(0.5f, 10f); pan += change } }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoom = 1f; pan = Offset.Zero }) },
    ) {
        val dims = design.dimensions
        val bounds = siteBounds(design)
        val minX = bounds.minX; val maxX = bounds.maxX; val minY = bounds.minY; val maxY = bounds.maxY
        val base = min((size.width - 24f) / (maxX - minX).toFloat().coerceAtLeast(1f), (size.height - 24f) / (maxY - minY).toFloat().coerceAtLeast(1f))
        val scale = base * zoom
        val midX = (minX + maxX) / 2
        val midY = (minY + maxY) / 2
        fun at(p: SitePoint) = Offset(size.width / 2 + ((p.x - midX) * scale).toFloat() + pan.x, size.height / 2 - ((p.y - midY) * scale).toFloat() + pan.y)
        fun poly(points: List<SitePoint>): Path = Path().apply { points.forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }; close() }

        // the ground and a 5 m grid
        drawPath(poly(design.terrain), hex(design.terrainColor, GroundColour).copy(alpha = 0.55f))
        var gx = kotlin.math.ceil(minX / 5.0) * 5.0
        while (gx <= maxX) { drawLine(Color(0x14FFFFFF), at(SitePoint(gx, minY)), at(SitePoint(gx, maxY)), 1f); gx += 5.0 }
        var gy = kotlin.math.ceil(minY / 5.0) * 5.0
        while (gy <= maxY) { drawLine(Color(0x14FFFFFF), at(SitePoint(minX, gy)), at(SitePoint(maxX, gy)), 1f); gy += 5.0 }
        drawPath(poly(design.terrain), Color(0xFF00E5FF), style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f))))

        // things on the ground
        design.features.forEach { f ->
            val colour = hex(f.color, featureColour(f.kind))
            when (f.kind) {
                "tree", "fountain" -> drawCircle(colour.copy(alpha = 0.85f), (f.width / 2 * scale).toFloat().coerceAtLeast(3f), at(SitePoint(f.x, f.y)))
                "lamp", "mast" -> { drawCircle(Color(0x33FFE9A8), (max(f.width, 0.3) * 1.6 * scale).toFloat().coerceAtLeast(6f), at(SitePoint(f.x, f.y))); drawCircle(colour, (max(f.width, 0.16) * scale).toFloat().coerceAtLeast(3f), at(SitePoint(f.x, f.y))) }
                "fence" -> drawPath(poly(rectCorners(f.copy(depth = max(f.depth, 0.15)))), colour)
                else -> drawPath(poly(rectCorners(f)), colour.copy(alpha = if (f.kind == "road" || f.kind == "path" || f.kind == "sidewalk") 0.9f else 1f))
            }
        }
        // buildings
        design.buildings.forEach { b ->
            drawPath(poly(b.points), Color(0xFF16232B))
            drawPath(poly(b.points), hex(b.color, WallColour), style = Stroke(max(2.5f, (b.thickness * scale).toFloat())))
            val cx = b.points.map { it.x }.average(); val cy = b.points.map { it.y }.average()
            val label = measurer.measure(b.name, TextStyle(color = Color(0xFFB8C7CE), fontSize = 11.sp))
            drawText(label, topLeft = at(SitePoint(cx, cy)) - Offset(label.size.width / 2f, label.size.height / 2f))
        }
        // cameras
        design.cameras.forEach { c ->
            val apex = RadarGeometry.toMetres(c.x, c.y, dims)
            val heading = RadarGeometry.headingOf(c.x, c.y, c.heading, dims)
            drawPath(poly(RadarGeometry.sector(apex, heading, (c.fov ?: 90.0) / 2, c.range ?: 12.0)), Color(0xFFFFB020).copy(alpha = 0.10f))
            drawCircle(Color(0xFFFFB020), 5f, at(apex))
        }
        // radars: their field and the body
        design.sensors.forEach { s ->
            val apex = RadarGeometry.toMetres(s.x, s.y, dims)
            val heading = RadarGeometry.headingOf(s.x, s.y, s.heading, dims)
            val (range, half) = RadarGeometry.view(s.kind)
            drawPath(poly(RadarGeometry.sector(apex, heading, half, range)), Color(0xFF00E5FF).copy(alpha = 0.13f))
            drawPath(poly(RadarGeometry.sector(apex, heading, half, range)), Color(0xFF00E5FF).copy(alpha = 0.55f), style = Stroke(1.5f))
            for (r in listOf(2.0, 4.0)) drawPath(Path().apply { RadarGeometry.sector(apex, heading, half, r).drop(1).forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }, Color(0x3300E5FF), style = Stroke(1f))
            drawRect(Color(0xFF00E5FF), at(apex) - Offset(6f, 6f), androidx.compose.ui.geometry.Size(12f, 12f))
            // radars of one node stand in one place: the name is written once
            if (design.sensors.takeWhile { it !== s }.none { o -> o.x == s.x && o.y == s.y }) {
                val label = design.sensors.filter { o -> o.x == s.x && o.y == s.y }.let { same -> if (same.size > 1) "${s.name.trimEnd('1', '2', '3', ' ')} ×${same.size}" else s.name }
                val name = measurer.measure(label, TextStyle(color = Color(0xFF8BD6FF), fontSize = 10.sp))
                drawText(name, topLeft = at(apex) + Offset(9f, -16f))
            }
        }
        // the people
        targets.forEach { t ->
            val p = at(t.at)
            val colour = if (t.counted) TargetColour else IgnoredColour
            if (t.counted) drawCircle(colour.copy(alpha = 0.25f * pulse.value), 22f * (0.7f + 0.5f * pulse.value), p)
            drawCircle(colour, 8f, p)
            drawCircle(Color.Black.copy(alpha = 0.6f), 8f, p, style = Stroke(1.5f))
            val id = measurer.measure("#${t.trackId}", TextStyle(color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center))
            drawText(id, topLeft = p + Offset(-id.size.width / 2f, 10f))
        }
        // a scale bar
        val metres = listOf(1.0, 2.0, 5.0, 10.0, 20.0, 50.0).lastOrNull { it * scale <= size.width * 0.28 } ?: 1.0
        val start = Offset(16f, size.height - 34f)
        drawLine(Color(0xFF8BD6FF), start, start + Offset((metres * scale).toFloat(), 0f), 3f)
        val label = measurer.measure("${metres.toInt()} m", TextStyle(color = Color(0xFF8BD6FF), fontSize = 10.sp))
        drawText(label, topLeft = start + Offset(0f, -label.size.height - 2f))
    }
}

@Composable
private fun rememberPulse(): State<Float> {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1100), androidx.compose.animation.core.RepeatMode.Reverse), label = "pulse")
}

private fun featureColour(kind: String): Color = when (kind) {
    "pillar" -> Color(0xFFA8B4B8); "lamp" -> Color(0xFF59666D); "mast" -> Color(0xFFC3D0D4); "solar" -> Color(0xFF173F7A); "canopy" -> Color(0xFF4B6F7A); "entrance" -> Color(0xFF9FB1B7)
    "path" -> Color(0xFF6F8B93); "road" -> Color(0xFF1C252B); "tree" -> Color(0xFF2F7D45); "kennel" -> Color(0xFFA6763F); "fence" -> Color(0xFFC9B48F); "fountain" -> Color(0xFFB3B8B3)
    "coop" -> Color(0xFFC9583B); "gate" -> Color(0xFF20272B); "sidewalk" -> Color(0xFFB8BCBD); else -> Color(0xFF8899A0)
}

// ---- 3D --------------------------------------------------------------------------------------------------------------------------

private class Projector(val yaw: Double, val pitch: Double, val cx: Double, val cy: Double, val scale: Double, val distance: Double, val width: Float, val height: Float) {
    class Point(val at: Offset, val depth: Double)
    fun project(x: Double, y: Double, z: Double): Point {
        val dx = x - cx
        val dy = y - cy
        val xr = dx * cos(yaw) - dy * sin(yaw)
        val yr = dx * sin(yaw) + dy * cos(yaw)
        val up = yr * sin(pitch) + z * cos(pitch)
        val away = yr * cos(pitch) - z * sin(pitch)
        val f = scale * distance / (distance + away)
        return Point(Offset(width / 2f + (xr * f).toFloat(), height * 0.56f - (up * f).toFloat()), away)
    }
}

private class Piece(val depth: Double, val draw: DrawScope.() -> Unit)

@Composable
private fun Site3D(design: SiteDesign, targets: List<PlacedTarget>, modifier: Modifier) {
    var yaw by remember { mutableDoubleStateOf(0.55) }
    var pitch by remember { mutableDoubleStateOf(0.95) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val pulse = rememberPulse()
    Canvas(
        modifier
            .pointerInput(Unit) { detectTransformGestures { _, change, zoomChange, _ -> yaw -= change.x * 0.008; pitch = (pitch + change.y * 0.006).coerceIn(0.25, 1.5); zoom = (zoom * zoomChange).coerceIn(0.5f, 5f) } }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { yaw = 0.55; pitch = 0.95; zoom = 1f }) },
    ) {
        val dims = design.dimensions
        val bounds = siteBounds(design)
        val cx = (bounds.minX + bounds.maxX) / 2
        val cy = (bounds.minY + bounds.maxY) / 2
        val spanX = (bounds.maxX - bounds.minX).coerceAtLeast(1.0)
        val spanY = (bounds.maxY - bounds.minY).coerceAtLeast(1.0)
        val radius = max(hypot(spanX, spanY) / 2, 8.0)
        // fit the width of the phone: a site is wider than tall on a tall screen
        val scale = size.width / (max(spanX, spanY) * 1.12) * zoom
        val cam = Projector(yaw, pitch, cx, cy, scale, radius * 3.0, size.width, size.height)
        fun P(x: Double, y: Double, z: Double = 0.0) = cam.project(x, y, z)
        fun path(points: List<Projector.Point>) = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.at.x, p.at.y) else lineTo(p.at.x, p.at.y) }; close() }
        val pieces = mutableListOf<Piece>()
        fun face(points: List<Triple<Double, Double, Double>>, colour: Color, edge: Color? = null) {
            val pts = points.map { P(it.first, it.second, it.third) }
            pieces += Piece(pts.map { it.depth }.average()) { drawPath(path(pts), colour); if (edge != null) drawPath(path(pts), edge, style = Stroke(1f)) }
        }
        fun box(x: Double, y: Double, w: Double, d: Double, rotation: Double, z0: Double, z1: Double, colour: Color) {
            val r = rotation * PI / 180
            val corners = listOf(-1 to -1, 1 to -1, 1 to 1, -1 to 1).map { (sx, sy) -> Pair(x + (sx * w / 2) * cos(r) - (sy * d / 2) * sin(r), y + (sx * w / 2) * sin(r) + (sy * d / 2) * cos(r)) }
            for (i in 0..3) {
                val a = corners[i]; val b = corners[(i + 1) % 4]
                val shade = 0.62f + 0.12f * i
                face(listOf(Triple(a.first, a.second, z0), Triple(b.first, b.second, z0), Triple(b.first, b.second, z1), Triple(a.first, a.second, z1)), colour.shade(shade))
            }
            face(corners.map { Triple(it.first, it.second, z1) }, colour.shade(1.05f))
        }

        // the ground, its grid and the flat things on it (drawn first, in order)
        val ground = design.terrain.map { P(it.x, it.y) }
        drawPath(path(ground), hex(design.terrainColor, GroundColour).copy(alpha = 0.75f))
        drawPath(path(ground), Color(0xFF00E5FF), style = Stroke(2f))
        var gx = 0.0
        while (gx <= dims.width) { drawLine(Color(0x12FFFFFF), P(gx, 0.0).at, P(gx, dims.depth).at, 1f); gx += 5.0 }
        var gy = 0.0
        while (gy <= dims.depth) { drawLine(Color(0x12FFFFFF), P(0.0, gy).at, P(dims.width, gy).at, 1f); gy += 5.0 }
        design.features.filter { it.kind in setOf("path", "road", "sidewalk", "entrance") }.forEach { f ->
            drawPath(path(rectCorners(f).map { P(it.x, it.y, 0.02) }), hex(f.color, featureColour(f.kind)).copy(alpha = 0.9f))
        }
        // the field of every radar and camera, on the ground
        design.cameras.forEach { c ->
            val apex = RadarGeometry.toMetres(c.x, c.y, dims)
            val heading = RadarGeometry.headingOf(c.x, c.y, c.heading, dims)
            drawPath(path(RadarGeometry.sector(apex, heading, (c.fov ?: 90.0) / 2, c.range ?: 12.0).map { P(it.x, it.y, 0.04) }), Color(0xFFFFB020).copy(alpha = 0.12f))
        }
        design.sensors.forEach { s ->
            val apex = RadarGeometry.toMetres(s.x, s.y, dims)
            val heading = RadarGeometry.headingOf(s.x, s.y, s.heading, dims)
            val (range, half) = RadarGeometry.view(s.kind)
            val fan = path(RadarGeometry.sector(apex, heading, half, range).map { P(it.x, it.y, 0.05) })
            drawPath(fan, Color(0xFF00E5FF).copy(alpha = 0.16f))
            drawPath(fan, Color(0xFF00E5FF).copy(alpha = 0.6f), style = Stroke(1.5f))
        }

        // buildings: walls, floors' worth of height, a roof
        design.buildings.forEach { b -> addBuilding(b, ::face) }
        // objects standing on the ground
        design.features.forEach { f ->
            val colour = hex(f.color, featureColour(f.kind))
            when (f.kind) {
                "tree" -> {
                    val trunk = P(f.x, f.y, 0.0); val top = P(f.x, f.y, f.height * 0.55); val crown = P(f.x, f.y, f.height * 0.75)
                    val r = (f.width / 2 * scale * (distanceFactor(cam, crown))).toFloat().coerceAtLeast(4f)
                    pieces += Piece(crown.depth) { drawLine(Color(0xFF6B4A2B), trunk.at, top.at, 5f); drawCircle(colour, r, crown.at); drawCircle(colour.shade(1.2f), r * 0.6f, crown.at - Offset(r * 0.2f, r * 0.25f)) }
                }
                "lamp", "mast" -> {
                    val a = P(f.x, f.y, 0.0); val b = P(f.x, f.y, f.height)
                    pieces += Piece(a.depth) { drawLine(Color(0xFF8A99A0), a.at, b.at, 4f); drawCircle(if (f.kind == "lamp") Color(0xFFFFE9A8) else colour, 6f, b.at) }
                }
                "pillar" -> box(f.x, f.y, f.width, f.depth, f.rotation, 0.0, f.height, colour)
                "kennel", "coop", "solar", "canopy" -> box(f.x, f.y, f.width, f.depth, f.rotation, if (f.kind == "canopy" || f.kind == "solar") 2.2 else 0.0, if (f.kind == "canopy") 2.4 else if (f.kind == "solar") 2.35 else max(f.height, 1.0), colour)
                "fence" -> box(f.x, f.y, f.width, max(f.depth, 0.12), f.rotation, 0.0, max(f.height, 1.0), colour)
                "gate" -> box(f.x, f.y, f.width, max(f.depth, 0.15), f.rotation, 0.0, max(f.height, 1.6), colour)
                "fountain" -> { val p = P(f.x, f.y, 0.4); pieces += Piece(p.depth) { drawCircle(colour, (f.width / 2 * scale).toFloat().coerceAtLeast(5f), p.at); drawCircle(Color(0xFF6EC6E8), (f.width / 3 * scale).toFloat().coerceAtLeast(3f), p.at) } }
            }
        }
        // radars and cameras: a post and a small body
        design.sensors.forEach { s ->
            val apex = RadarGeometry.toMetres(s.x, s.y, dims)
            val a = P(apex.x, apex.y, 0.0); val b = P(apex.x, apex.y, s.z)
            pieces += Piece(a.depth) { drawLine(Color(0xFF5A6A72), a.at, b.at, 3f); drawRect(Color(0xFF00E5FF), b.at - Offset(7f, 5f), androidx.compose.ui.geometry.Size(14f, 10f)) }
        }
        design.cameras.forEach { c ->
            val apex = RadarGeometry.toMetres(c.x, c.y, dims)
            val a = P(apex.x, apex.y, 0.0); val b = P(apex.x, apex.y, 2.4)
            pieces += Piece(a.depth) { drawLine(Color(0xFF5A6A72), a.at, b.at, 3f); drawCircle(Color(0xFFFFB020), 6f, b.at) }
        }
        // the people: a ring on the ground and a standing figure
        targets.forEach { t ->
            val foot = P(t.at.x, t.at.y, 0.0); val head = P(t.at.x, t.at.y, 1.7); val chest = P(t.at.x, t.at.y, 1.2)
            val colour = if (t.counted) TargetColour else IgnoredColour
            pieces += Piece(foot.depth - 0.5) {
                if (t.counted) drawCircle(colour.copy(alpha = 0.25f * pulse.value), 26f * (0.7f + 0.5f * pulse.value), foot.at)
                drawCircle(colour.copy(alpha = 0.5f), 12f, foot.at, style = Stroke(2f))
                drawLine(colour, foot.at, chest.at, 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawCircle(colour, 8f, head.at)
                drawCircle(Color.Black.copy(alpha = 0.55f), 8f, head.at, style = Stroke(1.5f))
            }
        }
        // far to near, so what is in front covers what is behind
        pieces.sortedByDescending { it.depth }.forEach { it.draw(this) }
    }
}

private fun distanceFactor(cam: Projector, p: Projector.Point): Double = cam.distance / (cam.distance + p.depth)

/** The walls of a building as shaded faces, and its roof: a ridge for a gable on a four-corner footprint, a point for hip and pyramid, flat for the rest. */
private fun addBuilding(b: SiteBuilding, face: (List<Triple<Double, Double, Double>>, Color, Color?) -> Unit) {
    val wall = hex(b.color, WallColour)
    val roof = hex(b.roofColor, RoofColour)
    val z0 = b.base
    val z1 = b.base + b.height
    val area = b.points.indices.sumOf { i -> val a = b.points[i]; val c = b.points[(i + 1) % b.points.size]; a.x * c.y - c.x * a.y }
    val ccw = area > 0
    for (i in b.points.indices) {
        val a = b.points[i]
        val c = b.points[(i + 1) % b.points.size]
        val dx = c.x - a.x
        val dy = c.y - a.y
        val len = hypot(dx, dy).coerceAtLeast(1e-6)
        val nx = (if (ccw) dy else -dy) / len
        val ny = (if (ccw) -dx else dx) / len
        val light = (nx * -0.5 + ny * -0.65).coerceAtLeast(0.0)
        face(listOf(Triple(a.x, a.y, z0), Triple(c.x, c.y, z0), Triple(c.x, c.y, z1), Triple(a.x, a.y, z1)), wall.shade((0.55 + 0.5 * light).toFloat()), Color(0x33000000))
    }
    val rise = { half: Double -> max(0.3, half * tan(b.roofSlope.coerceIn(3.0, 60.0) * PI / 180)) }
    val pts = b.points
    when {
        b.roofStyle == "flat" || b.roofStyle == "shed" || pts.size < 3 -> face(pts.map { Triple(it.x, it.y, z1 + if (b.roofStyle == "shed") 0.25 else 0.05) }, roof.shade(1.0f), Color(0x33000000))
        b.roofStyle == "gable" && pts.size == 4 -> {
            val len01 = hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y)
            val len12 = hypot(pts[2].x - pts[1].x, pts[2].y - pts[1].y)
            val q = if (len01 <= len12) pts else listOf(pts[1], pts[2], pts[3], pts[0])
            val shortSide = min(len01, len12)
            val h = z1 + rise(shortSide / 2)
            val r1 = SitePoint((q[0].x + q[1].x) / 2, (q[0].y + q[1].y) / 2)
            val r2 = SitePoint((q[2].x + q[3].x) / 2, (q[2].y + q[3].y) / 2)
            face(listOf(Triple(q[1].x, q[1].y, z1), Triple(q[2].x, q[2].y, z1), Triple(r2.x, r2.y, h), Triple(r1.x, r1.y, h)), roof.shade(0.9f), Color(0x33000000))
            face(listOf(Triple(q[3].x, q[3].y, z1), Triple(q[0].x, q[0].y, z1), Triple(r1.x, r1.y, h), Triple(r2.x, r2.y, h)), roof.shade(0.7f), Color(0x33000000))
            face(listOf(Triple(q[0].x, q[0].y, z1), Triple(q[1].x, q[1].y, z1), Triple(r1.x, r1.y, h)), wall.shade(0.8f), Color(0x33000000))
            face(listOf(Triple(q[2].x, q[2].y, z1), Triple(q[3].x, q[3].y, z1), Triple(r2.x, r2.y, h)), wall.shade(0.8f), Color(0x33000000))
        }
        else -> {
            val cxm = pts.map { it.x }.average(); val cym = pts.map { it.y }.average()
            val half = pts.minOf { hypot(it.x - cxm, it.y - cym) }
            val h = z1 + rise(half)
            for (i in pts.indices) {
                val a = pts[i]; val c = pts[(i + 1) % pts.size]
                face(listOf(Triple(a.x, a.y, z1), Triple(c.x, c.y, z1), Triple(cxm, cym, h)), roof.shade(0.65f + 0.1f * (i % 4)), Color(0x33000000))
            }
        }
    }
}

