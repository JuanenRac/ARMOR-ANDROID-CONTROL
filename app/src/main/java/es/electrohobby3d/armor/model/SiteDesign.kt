// ARMOR-ANDROID-CONTROL - the site as Studio designed it, the live radar targets, and the geometry that puts one on the other.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class SitePoint(val x: Double, val y: Double)
data class SiteDimensions(val width: Double, val depth: Double, val height: Double)
data class SiteBuilding(
    val id: String, val name: String, val points: List<SitePoint>, val base: Double, val floors: List<Double>, val thickness: Double,
    val color: String?, val roofStyle: String, val roofSlope: Double, val roofColor: String?,
) { val height: Double get() = floors.sum().coerceAtLeast(2.0) }
data class SiteFeature(val id: String, val kind: String, val x: Double, val y: Double, val z: Double, val width: Double, val depth: Double, val height: Double, val rotation: Double, val color: String?)
/** A radar of the design. x and y are percentages of the work area (as Studio stores them); [channel] is the radar number of its node (0: any). */
data class SiteSensor(
    val id: String, val name: String, val x: Double, val y: Double, val kind: String, val heading: Double?, val z: Double, val node: String?, val channel: Int, val mirror: Boolean,
)
data class SiteCamera(val id: String, val x: Double, val y: Double, val heading: Double?, val fov: Double?, val range: Double?)

data class SiteDesign(
    val dimensions: SiteDimensions, val terrain: List<SitePoint>, val terrainColor: String?, val buildings: List<SiteBuilding>,
    val features: List<SiteFeature>, val sensors: List<SiteSensor>, val cameras: List<SiteCamera>,
) {
    /** True when Studio has saved a design that has a radar: without one there is nothing to put the targets on. */
    val hasRadar: Boolean get() = sensors.isNotEmpty()
}

/** A target as the node reported it, in the radar's own frame: millimetres, x sideways and y ahead. */
data class LiveTarget(val sensorId: Int, val trackId: Int, val xMm: Double, val yMm: Double, val speedMmS: Double, val counted: Boolean)

/** A target on the site, in metres (x east, y north). */
data class PlacedTarget(val node: String, val sensorId: String, val trackId: Int, val at: SitePoint, val apex: SitePoint, val counted: Boolean, val inRated: Boolean, val speedMmS: Double)

object SiteParser {
    private fun JSONObject.points(key: String): List<SitePoint> = optJSONArray(key).objects().mapNotNull { p ->
        if (p.has("x") && p.has("y")) SitePoint(p.optDouble("x"), p.optDouble("y")) else null
    }
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until this@objects.length()) this@objects.optJSONObject(i)?.let(::add) }
    private fun JSONObject.colour(key: String): String? = optString(key, "").takeIf { Regex("^#[0-9a-fA-F]{6}$").matches(it) }?.lowercase()

    /** The server's answer to GET /api/v1/site ({"revision":..,"site":{..}}), or the site object itself. Null when nothing usable is there. */
    fun parse(document: JSONObject): SiteDesign? {
        val site = document.optJSONObject("site") ?: (if (document.has("dimensions") || document.has("sensors")) document else null) ?: return null
        val dims = site.optJSONObject("dimensions")
        val dimensions = SiteDimensions(dims?.optDouble("width", 60.0)?.coerceIn(5.0, 500.0) ?: 60.0, dims?.optDouble("depth", 40.0)?.coerceIn(5.0, 500.0) ?: 40.0, dims?.optDouble("height", 12.0) ?: 12.0)
        val terrainObject = site.optJSONObject("terrain")
        val terrain = terrainObject?.points("points").orEmpty().ifEmpty {
            listOf(SitePoint(0.0, 0.0), SitePoint(dimensions.width, 0.0), SitePoint(dimensions.width, dimensions.depth), SitePoint(0.0, dimensions.depth))
        }
        val buildings = site.optJSONArray("buildings").objects().mapNotNull { b ->
            val points = b.points("points")
            if (points.size < 3) return@mapNotNull null
            val roof = b.optJSONObject("roof")
            val floors = b.optJSONArray("floors")?.let { a -> (0 until a.length()).map { a.optDouble(it, 2.8).coerceIn(1.0, 20.0) } }.orEmpty().ifEmpty { listOf(2.8) }
            SiteBuilding(
                b.optString("id"), b.optString("name"), points, b.optDouble("base", 0.0), floors, b.optDouble("thickness", 0.25), b.colour("color"),
                roof?.optString("style", "flat") ?: "flat", roof?.optDouble("slope", 20.0) ?: 20.0, b.colour("roofColor"),
            )
        }
        val features = site.optJSONArray("features").objects().map { f ->
            SiteFeature(
                f.optString("id"), f.optString("kind"), f.optDouble("x"), f.optDouble("y"), f.optDouble("z", 0.0), f.optDouble("width", 1.0), f.optDouble("depth", 1.0),
                f.optDouble("height", 1.0), f.optDouble("rotation", 0.0), f.colour("color"),
            )
        }
        val sensors = site.optJSONArray("sensors").objects().mapNotNull { s ->
            val id = s.optString("id")
            if (id.isBlank() || !s.has("x") || !s.has("y")) return@mapNotNull null
            SiteSensor(
                id, s.optString("name", id), s.optDouble("x"), s.optDouble("y"), if (s.optString("kind") == "LD2461") "LD2461" else "LD2450",
                if (s.has("heading") && !s.isNull("heading")) s.optDouble("heading") else null, s.optDouble("z", 1.5),
                s.optString("node", "").ifBlank { null }, s.optInt("channel", 0), s.optBoolean("mirror", false),
            )
        }
        val places = site.optJSONObject("cameraPlacements")
        val cameras = places?.keys()?.asSequence()?.mapNotNull { id ->
            val p = places.optJSONObject(id) ?: return@mapNotNull null
            if (!p.has("x") || !p.has("y")) null
            else SiteCamera(id, p.optDouble("x"), p.optDouble("y"), if (p.has("heading")) p.optDouble("heading") else null, if (p.has("fov")) p.optDouble("fov") else null, if (p.has("range")) p.optDouble("range") else null)
        }?.toList().orEmpty()
        return SiteDesign(dimensions, terrain, terrainObject?.colour("color"), buildings, features, sensors, cameras)
    }

    /** The targets of a node's state object (`targets` of GET /api/v1/status). */
    fun targets(node: JSONObject): List<LiveTarget> = node.optJSONArray("targets").objects().map { t ->
        LiveTarget(t.optInt("sensor_id"), t.optInt("track_id"), t.optDouble("x_mm"), t.optDouble("y_mm"), t.optDouble("speed_mm_s"), t.optBoolean("counted", true))
    }
}

/** The same rules as Studio's designer/model.ts and radarMap.ts, so the phone shows what the plan on the computer shows. */
object RadarGeometry {
    /** What a model is rated for (its manufacturer's figures): range in metres and half of the horizontal angle in degrees. */
    fun view(kind: String): Pair<Double, Double> = if (kind == "LD2461") 8.0 to 45.0 else 6.0 to 60.0

    /** A radar or camera (percent of the work area) as a point in metres. */
    fun toMetres(x: Double, y: Double, dims: SiteDimensions) = SitePoint(x / 100.0 * dims.width, (1.0 - y / 100.0) * dims.depth)

    /** Where it faces, in degrees anticlockwise from east; unless set, toward the middle of the work area. */
    fun headingOf(x: Double, y: Double, heading: Double?, dims: SiteDimensions): Double {
        if (heading != null && heading.isFinite()) return ((heading % 360.0) + 360.0) % 360.0
        val at = toMetres(x, y, dims)
        val dx = dims.width / 2 - at.x
        val dy = dims.depth / 2 - at.y
        if (hypot(dx, dy) < 1e-6) return 90.0
        return ((atan2(dy, dx) * 180.0 / PI) + 360.0) % 360.0
    }

    /** A target in the radar's frame as a point of the site. Which side positive x lies on is an assumption (to the right of where the radar faces) unless [mirror]. */
    fun targetToSite(apex: SitePoint, headingDeg: Double, xMm: Double, yMm: Double, mirror: Boolean): SitePoint {
        val forward = yMm / 1000.0
        val sideways = (if (mirror) -1.0 else 1.0) * xMm / 1000.0
        val h = headingDeg * PI / 180.0
        return SitePoint(apex.x + cos(h) * forward + sin(h) * sideways, apex.y + sin(h) * forward - cos(h) * sideways)
    }

    fun inSector(point: SitePoint, apex: SitePoint, headingDeg: Double, halfAngleDeg: Double, rangeM: Double): Boolean {
        val dx = point.x - apex.x
        val dy = point.y - apex.y
        if (hypot(dx, dy) > rangeM) return false
        val angle = atan2(dy, dx) * 180.0 / PI
        return kotlin.math.abs(((angle - headingDeg + 540.0) % 360.0) - 180.0) <= halfAngleDeg
    }

    /** The corners of a detection sector: the apex, then the arc. */
    fun sector(apex: SitePoint, headingDeg: Double, halfAngleDeg: Double, rangeM: Double, steps: Int = 24): List<SitePoint> = buildList {
        add(apex)
        for (i in 0..steps) {
            val a = (headingDeg - halfAngleDeg + 2 * halfAngleDeg * i / steps) * PI / 180.0
            add(SitePoint(apex.x + cos(a) * rangeM, apex.y + sin(a) * rangeM))
        }
    }

    /** The radar of the design that produces a node's radar number: the one wired to that node and channel, else one of the node with no channel. */
    fun sensorFor(sensors: List<SiteSensor>, node: String, channel: Int): SiteSensor? {
        val wired = sensors.filter { it.node == node }
        return wired.firstOrNull { it.channel != 0 && it.channel == channel } ?: wired.firstOrNull { it.channel == 0 }
    }

    /** Every live target of the nodes that are online and not stale, that can be placed; the rest are counted apart. */
    fun place(design: SiteDesign, nodes: List<FieldNode>): Pair<List<PlacedTarget>, Int> {
        val placed = mutableListOf<PlacedTarget>()
        var missing = 0
        for (node in nodes) {
            if (!node.online || node.stale) continue
            for (target in node.targets) {
                val sensor = sensorFor(design.sensors, node.id, target.sensorId)
                if (sensor == null) { missing++; continue }
                val apex = toMetres(sensor.x, sensor.y, design.dimensions)
                val heading = headingOf(sensor.x, sensor.y, sensor.heading, design.dimensions)
                val at = targetToSite(apex, heading, target.xMm, target.yMm, sensor.mirror)
                val (range, half) = view(sensor.kind)
                placed += PlacedTarget(node.id, sensor.id, target.trackId, at, apex, target.counted, inSector(at, apex, heading, half, range), target.speedMmS)
            }
        }
        return placed to missing
    }
}
