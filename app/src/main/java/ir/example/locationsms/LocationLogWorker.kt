package ir.example.locationsms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class LocationLogWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val location = LocationHelper.getCurrentLocation(applicationContext)
            ?: return Result.retry()

        LocationLogger.appendEntry(applicationContext, location)
        return Result.success()
    }
}
