package com.exosystems.exopillclientsample.bridge

import android.content.Context
import android.content.SharedPreferences

/**
 * submit_result로 전달된 마지막 결과를 단순 저장/조회하는 SharedPreferences 기반 저장소.
 */
data class StoredResult(
    /** B가 보낸 요청 ID(없으면 빈 문자열) */
    val requestId: String,
    /** B가 보낸 payload(JSON 문자열) */
    val payload: String,
    /** 저장 시각(UTC millis) */
    val updatedAtMillis: Long
)

/**
 * 결과 데이터 영속화 도우미. call() 처리 흐름에서 동기 저장용으로만 사용한다.
 */
class ResultRepository(context: Context) {

    /** 마지막 결과를 저장하는 내부 SharedPreferences */
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 결과를 즉시 커밋 방식으로 저장. 실패 시 false 반환. */
    fun saveResult(requestId: String, payload: String): Boolean {
        return prefs.edit()
            .putString(KEY_LAST_REQUEST_ID, requestId)
            .putString(KEY_LAST_PAYLOAD, payload)
            .putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    /** 마지막으로 저장된 결과를 조회. 없으면 null 반환. */
    fun getLastResult(): StoredResult? {
        val payload = prefs.getString(KEY_LAST_PAYLOAD, null) ?: return null
        val requestId = prefs.getString(KEY_LAST_REQUEST_ID, "") ?: ""
        val updatedAt = prefs.getLong(KEY_LAST_UPDATED_AT, 0L)
        return StoredResult(requestId = requestId, payload = payload, updatedAtMillis = updatedAt)
    }

    companion object {
        /** SharedPreferences 파일명 */
        private const val PREFS_NAME = "bridge_storage"
        /** 마지막 요청 ID 키 */
        private const val KEY_LAST_REQUEST_ID = "last_request_id"
        /** 마지막 payload 키 */
        private const val KEY_LAST_PAYLOAD = "last_payload"
        /** 마지막 업데이트 시각 키 */
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
    }
}
