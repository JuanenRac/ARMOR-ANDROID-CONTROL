package es.electrohobby3d.armor

import es.electrohobby3d.armor.model.FieldNode
import es.electrohobby3d.armor.model.LiveTarget
import es.electrohobby3d.armor.model.RadarGeometry
import es.electrohobby3d.armor.model.SiteDimensions
import es.electrohobby3d.armor.model.SiteParser
import es.electrohobby3d.armor.model.SitePoint
import es.electrohobby3d.armor.model.SiteSensor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The same expectations as Studio's designer tests: the phone must put a person where the plan on the computer puts them. */
class RadarGeometryTest {
    private val dims = SiteDimensions(60.0, 40.0, 3.0)
    // at (10, 10) m facing east
    private fun sensor(id: String = "s1", node: String? = "gate", channel: Int = 0, mirror: Boolean = false, kind: String = "LD2450") =
        SiteSensor(id, "Gate", 10.0 / 60.0 * 100.0, 75.0, kind, 0.0, 1.5, node, channel, mirror)
    private fun node(id: String = "gate", online: Boolean = true, stale: Boolean = false, targets: List<LiveTarget> = listOf(LiveTarget(1, 4, 0.0, 3000.0, 0.0, true))) =
        FieldNode(id, online, 5.0, targets.size, "review", stale, targets)
    private fun design(vararg sensors: SiteSensor) = es.electrohobby3d.armor.model.SiteDesign(dims, emptyList(), null, emptyList(), emptyList(), sensors.toList(), emptyList())

    @Test fun `a radar or camera in percent is a point in metres`() {
        val p = RadarGeometry.toMetres(50.0, 25.0, dims)
        assertEquals(30.0, p.x, 1e-9)
        assertEquals(30.0, p.y, 1e-9)   // y is counted from the top of the plan: 25 % from the top is 30 m of 40 from the south
    }

    @Test fun `the default facing is toward the middle and an explicit facing wins`() {
        assertEquals(90.0, RadarGeometry.headingOf(50.0, 50.0, null, dims), 1e-9)
        assertEquals(0.0, RadarGeometry.headingOf(0.0, 50.0, null, dims), 1e-9)          // west edge, middle: faces east
        assertEquals(270.0, RadarGeometry.headingOf(10.0, 10.0, -90.0, dims), 1e-9)
    }

    @Test fun `a target ahead of a radar that faces east is east of it, and the sideways axis is to the right`() {
        val apex = SitePoint(10.0, 10.0)
        val ahead = RadarGeometry.targetToSite(apex, 0.0, 0.0, 3000.0, false)
        assertEquals(13.0, ahead.x, 1e-9); assertEquals(10.0, ahead.y, 1e-9)
        val right = RadarGeometry.targetToSite(apex, 0.0, 1000.0, 2000.0, false)
        assertEquals(12.0, right.x, 1e-9); assertEquals(9.0, right.y, 1e-9)              // 1 m to the right of east is south
        val mirrored = RadarGeometry.targetToSite(apex, 0.0, 1000.0, 2000.0, true)
        assertEquals(11.0, mirrored.y, 1e-9)
        val north = RadarGeometry.targetToSite(apex, 90.0, -500.0, 1000.0, false)
        assertEquals(9.5, north.x, 1e-9); assertEquals(11.0, north.y, 1e-9)              // facing north, half a metre to the left is west
    }

    @Test fun `the sector of each model`() {
        assertEquals(6.0 to 60.0, RadarGeometry.view("LD2450"))
        assertEquals(8.0 to 45.0, RadarGeometry.view("LD2461"))
        val apex = SitePoint(0.0, 0.0)
        assertTrue(RadarGeometry.inSector(SitePoint(5.0, 0.0), apex, 0.0, 60.0, 6.0))
        assertFalse(RadarGeometry.inSector(SitePoint(7.0, 0.0), apex, 0.0, 60.0, 6.0))
        assertFalse(RadarGeometry.inSector(SitePoint(0.0, 5.0), apex, 0.0, 60.0, 6.0))
        assertEquals(26, RadarGeometry.sector(apex, 0.0, 60.0, 6.0).size)
    }

    @Test fun `the radar wired to a node is found by its number, then by any`() {
        val any = sensor("any"); val second = sensor("second", channel = 2)
        assertEquals("second", RadarGeometry.sensorFor(listOf(any, second), "gate", 2)?.id)
        assertEquals("any", RadarGeometry.sensorFor(listOf(any, second), "gate", 1)?.id)
        assertNull(RadarGeometry.sensorFor(listOf(any), "other", 1))
        assertNull(RadarGeometry.sensorFor(listOf(sensor(node = null)), "gate", 1))
    }

    @Test fun `targets are placed on the site, and silent nodes and unwired ones are left out`() {
        val (placed, missing) = RadarGeometry.place(design(sensor()), listOf(node()))
        assertEquals(1, placed.size); assertEquals(0, missing)
        assertEquals(13.0, placed[0].at.x, 1e-6); assertEquals(10.0, placed[0].at.y, 1e-6)
        assertTrue(placed[0].inRated); assertTrue(placed[0].counted); assertEquals(4, placed[0].trackId)
        val far = RadarGeometry.place(design(sensor()), listOf(node(targets = listOf(LiveTarget(1, 1, 0.0, 7000.0, 0.0, false)))))
        assertFalse(far.first[0].inRated); assertFalse(far.first[0].counted)
        assertEquals(0 to 1, RadarGeometry.place(design(sensor()), listOf(node(id = "north"))).let { it.first.size to it.second })
        assertTrue(RadarGeometry.place(design(sensor()), listOf(node(stale = true))).first.isEmpty())
        assertTrue(RadarGeometry.place(design(sensor()), listOf(node(online = false))).first.isEmpty())
    }

    @Test fun `the site document of the server is read`() {
        val document = JSONObject("""
            {"revision":3,"site":{"schema":"armor-studio/site/1","dimensions":{"width":60,"depth":40,"height":12},
             "terrain":{"points":[{"x":0,"y":0},{"x":60,"y":0},{"x":60,"y":40},{"x":0,"y":40}],"color":"#17414D"},
             "buildings":[{"id":"b1","name":"House","points":[{"x":16,"y":12},{"x":28,"y":12},{"x":28,"y":20},{"x":16,"y":20}],"base":0,"floors":[2.8,2.8],"thickness":0.25,"roof":{"style":"gable","slope":30,"overhang":0.4,"ridge":0},"color":"#d9e3e6"}],
             "features":[{"id":"t1","kind":"tree","x":10,"y":26,"z":0,"width":4,"depth":4,"height":6,"rotation":0,"slope":0,"style":"oak","color":"#7a2cff"}],
             "sensors":[{"id":"s1","name":"Gate","x":16.7,"y":75,"kind":"LD2461","heading":0,"node":"perimetro-1","channel":2,"mirror":true}],
             "cameraPlacements":{"cam1":{"x":50,"y":10,"heading":180,"fov":80}}}}
        """)
        val design = SiteParser.parse(document)!!
        assertEquals(60.0, design.dimensions.width, 0.0)
        assertEquals("#17414d", design.terrainColor)
        assertEquals(4, design.terrain.size)
        assertEquals(1, design.buildings.size); assertEquals(5.6, design.buildings[0].height, 1e-9); assertEquals("gable", design.buildings[0].roofStyle)
        assertEquals("#7a2cff", design.features[0].color)
        assertTrue(design.hasRadar)
        val s = design.sensors[0]
        assertEquals("LD2461", s.kind); assertEquals("perimetro-1", s.node); assertEquals(2, s.channel); assertTrue(s.mirror)
        assertEquals("cam1", design.cameras[0].id); assertEquals(80.0, design.cameras[0].fov!!, 0.0)
    }

    @Test fun `no design saved, or an empty one, gives nothing to draw on`() {
        assertNull(SiteParser.parse(JSONObject("""{"revision":0,"updated_at":null,"updated_by":null,"site":null}""")))
        val empty = SiteParser.parse(JSONObject("""{"revision":1,"site":{"schema":"armor-studio/site/1"}}"""))
        assertNotNull(empty)
        assertFalse(empty!!.hasRadar)
        assertEquals(4, empty.terrain.size)   // a plain rectangle of the default size
    }

    @Test fun `targets of a node state are read`() {
        val node = JSONObject("""{"online":true,"targets":[{"sensor_id":2,"track_id":7,"x_mm":-300,"y_mm":1500,"speed_mm_s":120,"counted":false}]}""")
        val t = SiteParser.targets(node)
        assertEquals(1, t.size); assertEquals(2, t[0].sensorId); assertEquals(7, t[0].trackId); assertEquals(-300.0, t[0].xMm, 0.0); assertFalse(t[0].counted)
        assertTrue(SiteParser.targets(JSONObject("""{"online":true}""")).isEmpty())
    }
}
