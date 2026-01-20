package com.exosystems.exopillclientsample.bridge

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * submit_result 이후 payload를 비동기 처리(예: 서버 업로드)하기 위한 워커 스켈레톤.
 */
class BridgeResultSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * 입력 Data에 포함된 payload를 읽어 서버 전송 등의 작업을 수행한다.
     * 현재는 TODO 상태로 성공을 반환만 한다.
     */
    override suspend fun doWork(): Result {
        val payload = inputData.getString(ABridgeContract.EXTRA_PAYLOAD).orEmpty()
        if (payload.isBlank()) {
            Log.w(TAG, "No payload passed to worker; failing")
            return Result.failure()
        }

        // TODO Implement server sync for payload and requestId via API client.
        Log.d(TAG, "Queued payload for async processing (requestId=${'$'}{inputData.getString(ABridgeContract.EXTRA_REQUEST_ID)})")
        return Result.success()
    }

    companion object {
        /** Log 태그 */
        const val TAG: String = "BridgeResultSyncWorker"
    }
}
