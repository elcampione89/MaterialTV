package com.hasanege.materialtv.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val SYNC_WORK_NAME = "catalog_sync_work"

    fun schedulePeriodicSync(context: Context, profileId: String, username: String, password: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val inputData = Data.Builder()
            .putString("PROFILE_ID", profileId)
            .putString("USERNAME", username)
            .putString("PASSWORD", password)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )

        // TV sticks are low on CPU/RAM: skip the background metadata enrichment there
        if (com.hasanege.materialtv.utils.TvUtils.isHardwareTv(context)) {
            WorkManager.getInstance(context).cancelUniqueWork("details_enrichment_work")
            return
        }

        val enrichmentRequest = PeriodicWorkRequestBuilder<DetailsEnrichmentWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "details_enrichment_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            enrichmentRequest
        )
    }
}
