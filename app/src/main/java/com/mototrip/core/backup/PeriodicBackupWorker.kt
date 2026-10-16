package com.mototrip.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mototrip.data.local.MotoTripDatabase
import java.io.File
import java.util.concurrent.TimeUnit

class PeriodicBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = MotoTripDatabase.getInstance(applicationContext)
            val snapshot = BackupManager.createDatabaseSnapshot(database)

            // Only make backup if there are trips recorded
            if (snapshot.trips.isNotEmpty()) {
                val json = BackupManager.serializeBackup(snapshot)
                val backupDir = File(applicationContext.filesDir, "backups").apply { mkdirs() }
                val backupFile = File(backupDir, "mototrip_backup_${System.currentTimeMillis()}.json")
                backupFile.writeText(json)

                // Retain only last 5 backup snapshots
                val allBackups = backupDir.listFiles { file -> file.name.startsWith("mototrip_backup_") }
                if (allBackups != null && allBackups.size > 5) {
                    allBackups.sortedBy { it.lastModified() }
                        .take(allBackups.size - 5)
                        .forEach { it.delete() }
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "mototrip_periodic_backup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
