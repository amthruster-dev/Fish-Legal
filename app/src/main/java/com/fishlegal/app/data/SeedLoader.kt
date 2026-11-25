package com.fishlegal.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the bundled starter dataset (California ocean/bay + inland species and
 * regulations, sourced from CDFW) into Room on first launch, so the app isn't
 * empty out of the box. Sync later replaces/expands this as real updates come
 * from the backend, but this bundled copy is what makes the offline experience
 * usable immediately after install.
 *
 * IMPORTANT: this is a general-statewide-default dataset, not a replacement for
 * the full CDFW regulations booklet. Many specific lakes/rivers/zones carry
 * their own exceptions. Treat rule.notes as the authoritative caveat text.
 */
object SeedLoader {

    private const val PREFS = "seed"
    private const val KEY_SEEDED_VERSION = "seeded_version"
    private const val CURRENT_SEED_VERSION = 1 // bump when seed/*.json changes meaningfully

    suspend fun seedIfNeeded(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seededVersion = prefs.getInt(KEY_SEEDED_VERSION, 0)
        if (seededVersion >= CURRENT_SEED_VERSION) return

        val species = loadSpecies(context)
        val zones = loadZones(context)
        val rules = loadRules(context)

        db.speciesDao().upsertAll(species)
        // Ocean zone first: when a point could match more than one zone (coastal
        // overlap), ZoneLookup.findZone returns the first match, and ocean rules
        // should win over the broader inland polygon near the coast.
        db.regulationDao().upsertZones(zones.sortedBy { if (it.zoneId == "ca_ocean") 0 else 1 })
        db.regulationDao().upsertRules(rules)

        prefs.edit().putInt(KEY_SEEDED_VERSION, CURRENT_SEED_VERSION).apply()
    }

    private fun loadSpecies(context: Context): List<Species> {
        val json = JSONArray(context.assets.open("seed/species.json").bufferedReader().readText())
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Species(
                id = o.getString("id"),
                commonName = o.getString("commonName"),
                scientificName = o.getString("scientificName"),
                family = o.optString("family", ""),
                imageAssetPath = o.optString("imageAssetPath", null.toString()).takeIf { o.has("imageAssetPath") },
                description = o.optString("description", null.toString()).takeIf { o.has("description") },
                modelLabelIndex = o.getInt("modelLabelIndex")
            )
        }
    }

    private fun loadZones(context: Context): List<RegulationZone> {
        val json = JSONArray(context.assets.open("seed/zones.json").bufferedReader().readText())
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            val polygon = o.getJSONArray("polygon")
            // Store as [lon, lat] pairs to match ZoneLookup's expected format.
            val flat = JSONArray()
            for (p in 0 until polygon.length()) flat.put(polygon.getJSONArray(p))
            RegulationZone(
                zoneId = o.getString("zoneId"),
                name = o.getString("name"),
                state = o.getString("state"),
                polygonGeoJson = flat.toString(),
                lastUpdated = o.getLong("lastUpdated")
            )
        }
    }

    private fun loadRules(context: Context): List<RegulationRule> {
        val json = JSONArray(context.assets.open("seed/rules.json").bufferedReader().readText())
        val now = System.currentTimeMillis()
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            RegulationRule(
                zoneId = o.getString("zoneId"),
                speciesId = o.getString("speciesId"),
                minSizeInches = if (o.isNull("minSizeInches")) null else o.getDouble("minSizeInches"),
                maxSizeInches = if (o.isNull("maxSizeInches")) null else o.getDouble("maxSizeInches"),
                dailyBagLimit = if (o.isNull("dailyBagLimit")) null else o.getInt("dailyBagLimit"),
                openSeasonStart = if (o.isNull("openSeasonStart")) null else o.getString("openSeasonStart"),
                openSeasonEnd = if (o.isNull("openSeasonEnd")) null else o.getString("openSeasonEnd"),
                isClosed = o.getBoolean("isClosed"),
                notes = o.optString("notes", null.toString()).takeIf { o.has("notes") },
                sourceUrl = o.optString("sourceUrl", null.toString()).takeIf { o.has("sourceUrl") },
                lastUpdated = now
            )
        }
    }
}
