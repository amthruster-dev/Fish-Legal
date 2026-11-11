package com.fishlegal.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fishlegal.app.R
import com.fishlegal.app.data.AppDatabase
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val speciesId = intent.getStringExtra(EXTRA_SPECIES_ID)
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)

        val nameView = findViewById<android.widget.TextView>(R.id.tvSpeciesName)
        val regView = findViewById<android.widget.TextView>(R.id.tvRegulationSummary)

        lifecycleScope.launch {
            val db = AppDatabase.get(applicationContext)
            val species = speciesId?.let { db.speciesDao().getById(it) }
            val rule = if (speciesId != null && zoneId != null) {
                db.regulationDao().getRule(zoneId, speciesId)
            } else null

            nameView.text = species?.let { "${it.commonName} (${it.scientificName})" }
                ?: "Species not recognized (confidence ${(confidence * 100).toInt()}%)"

            regView.text = when {
                zoneId == null -> "Location unavailable — can't look up local regulations."
                rule == null -> "No regulation data on file for this zone/species."
                rule.isClosed -> "Season CLOSED for this species here."
                else -> buildString {
                    rule.minSizeInches?.let { append("Min size: ${it}in\n") }
                    rule.maxSizeInches?.let { append("Max size: ${it}in\n") }
                    rule.dailyBagLimit?.let { append("Bag limit: $it/day\n") }
                    if (rule.openSeasonStart != null && rule.openSeasonEnd != null) {
                        append("Season: ${rule.openSeasonStart} - ${rule.openSeasonEnd}")
                    }
                }
            }

            NotificationHelper.showCatchResult(applicationContext, species, rule)
        }
    }

    companion object {
        const val EXTRA_SPECIES_ID = "species_id"
        const val EXTRA_CONFIDENCE = "confidence"
        const val EXTRA_ZONE_ID = "zone_id"
    }
}
