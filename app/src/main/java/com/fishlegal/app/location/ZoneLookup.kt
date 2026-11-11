package com.fishlegal.app.location

import com.fishlegal.app.data.RegulationZone
import org.json.JSONArray

/**
 * Offline point-in-polygon lookup against locally stored zone boundaries.
 * No network call — works fully offline using whatever zones were last synced.
 */
object ZoneLookup {

    fun findZone(lat: Double, lon: Double, zones: List<RegulationZone>): RegulationZone? {
        return zones.firstOrNull { zone -> pointInPolygon(lat, lon, zone.polygonGeoJson) }
    }

    private fun pointInPolygon(lat: Double, lon: Double, geoJson: String): Boolean {
        // Expects polygonGeoJson as a flat JSON array of [lon, lat] pairs.
        val coords = JSONArray(geoJson)
        var inside = false
        var j = coords.length() - 1
        for (i in 0 until coords.length()) {
            val pi = coords.getJSONArray(i)
            val pj = coords.getJSONArray(j)
            val xi = pi.getDouble(0); val yi = pi.getDouble(1)
            val xj = pj.getDouble(0); val yj = pj.getDouble(1)

            val intersects = ((yi > lat) != (yj > lat)) &&
                (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}
