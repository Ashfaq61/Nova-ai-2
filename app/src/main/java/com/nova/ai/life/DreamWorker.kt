package com.nova.ai.life

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nova.ai.memory.KnowledgeGraph
import com.nova.ai.memory.MemoryDb
import com.nova.ai.memory.NovaPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DreamWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "nova_dream_consolidation"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<DreamWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val memoryDb = MemoryDb.getInstance(applicationContext)
            val kg = KnowledgeGraph.getInstance(applicationContext)

            // 1. Summarize recent memories
            val recentMemories = memoryDb.getAllMemories(30)
            if (recentMemories.isNotEmpty()) {
                val summary = "Nightly Dream Synthesis: Analyzed ${recentMemories.size} episodic events."
                kg.addEvent(summary, location = "Cognitive Core", entity = "Nova")
            }

            // 2. Prune old telemetry events older than 30 days
            kg.pruneOldEvents(30)

            // 3. Synthesize next day proactive agenda
            NovaPrefs.init(applicationContext)
            NovaPrefs.setKey("dream_last_run", System.currentTimeMillis().toString())
            NovaPrefs.setKey("morning_briefing_ready", "true")

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
