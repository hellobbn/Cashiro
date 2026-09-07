package com.ritesh.cashiro.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OptimizedSmsReaderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()

    companion object {
        const val WORK_NAME = "optimized_sms_reader"
        const val INPUT_FORCE_RESYNC = "force_resync"
        const val PROGRESS_TOTAL = "total"
        const val PROGRESS_PROCESSED = "processed"
        const val PROGRESS_PARSED = "parsed"
        const val PROGRESS_SAVED = "saved"
        const val PROGRESS_TIME_ELAPSED = "time_elapsed"
        const val PROGRESS_ESTIMATED_TIME_REMAINING = "eta"
        const val PROGRESS_CURRENT_BATCH = "current_batch"
        const val PROGRESS_TOTAL_BATCHES = "total_batches"
    }
}
