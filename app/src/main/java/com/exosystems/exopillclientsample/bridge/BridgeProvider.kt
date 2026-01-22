package com.exosystems.exopillclientsample.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * B 앱이 호출하는 ContentProvider. caller 서명 검증 후 request_data / submit_result IPC를 처리한다.
 */
class BridgeProvider : ContentProvider() {

    /** 앱 전역 Context (null 방어 포함) */
    private val appContext: Context
        get() = context?.applicationContext
            ?: throw IllegalStateException("BridgeProvider context is null")

    /** 결과 저장용 로컬 저장소 */
    private val repository: ResultRepository by lazy { ResultRepository(appContext) }

    /** Provider 초기화. 별도 작업 없음 */
    override fun onCreate(): Boolean = true

    /** ContentResolver.call 엔트리. 호출자 검증 후 메서드별 로직을 분기 처리한다. */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return errorBundle("Context unavailable")
        enforceCallerAllowed(ctx)
        return try {
            when (method) {
                ABridgeContract.METHOD_REQUEST_DATA -> handleRequestData(extras)
                ABridgeContract.METHOD_SUBMIT_RESULT -> handleSubmitResult(extras)
                else -> errorBundle("Unknown method: ${'$'}safeMethod")
            }
        } catch (se: SecurityException) {
            throw se
        } catch (t: Throwable) {
            Log.e(TAG, "call(${'$'}safeMethod) failed", t)
            errorBundle("call failed: ${'$'}{t.message}")
        }
    }

    /** request_data 처리: requestId를 포함한 샘플 JSON을 반환. */
    private fun handleRequestData(extras: Bundle?): Bundle {
        val requestId = extras?.getString(ABridgeContract.EXTRA_REQUEST_ID).orEmpty()
        val dataJson = loadDataForB(requestId)
        return bundleOf(
            ABridgeContract.KEY_OK to true,
            ABridgeContract.KEY_DATA to dataJson
        )
    }

    /** B에 제공할 JSON 생성. 최근 저장 결과가 있으면 포함한다. */
    private fun loadDataForB(requestId: String): String {
        val json = JSONObject()
        json.put("requestId", requestId)
        json.put("timestamp", System.currentTimeMillis())

        // SharedPreferences에서 환자 정보 읽기
        val sharedPreferences = appContext.getSharedPreferences("patient_prefs", Context.MODE_PRIVATE)
        val patientId = sharedPreferences.getString("patient_id", null)
        val hospitalName = sharedPreferences.getString("hospital_name", null)

        if (patientId != null && hospitalName != null) {
            // 환자 정보가 있으면 message에 JSON 형태로 담기
            val patientJson = JSONObject()
            patientJson.put("patientId", patientId)
            patientJson.put("hospitalName", hospitalName)
            json.put("message", patientJson.toString())
        } else {
            // 환자 정보가 없으면 message를 비워두거나 특정 메시지 전달
            json.put("message", "")
        }

        repository.getLastResult()?.let { last ->
            json.put("lastPayload", last.payload)
            json.put("lastRequestId", last.requestId)
            json.put("lastUpdatedAt", last.updatedAtMillis)
        }

        val dataJson = json.toString()
        Log.d(TAG, "IPC Provider: Sent Patient Data: $dataJson") // 로그 추가
        return dataJson
    }

    /** submit_result 처리: payload 검증 후 로컬 저장 및 워크 큐에 등록. */
    private fun handleSubmitResult(extras: Bundle?): Bundle {
        val payload = extras?.getString(ABridgeContract.EXTRA_PAYLOAD)
        if (payload.isNullOrBlank()) {
            Log.e(TAG, "IPC Provider: Received empty payload for electric result.") // 로그 추가
            return errorBundle("payload is required")
        }
        // val requestId = extras?.getString(ABridgeContract.EXTRA_REQUEST_ID).orEmpty() // 이 변수는 사용되지 않으므로 제거

        // 전기자극 결과 파싱 및 SharedPreferences 저장
        return try {
            val json = JSONObject(payload)
            val id = json.optLong("id")
            val progressTime = json.optLong("progressTime")
            val modeId = json.optLong("modeId")

            Log.d(TAG, "IPC Provider: Received Electric Result - ID: $id, Time: $progressTime, Mode: $modeId") // 로그 추가

            val electricResultPrefs = appContext.getSharedPreferences("electric_result_prefs", Context.MODE_PRIVATE)
            with(electricResultPrefs.edit()) {
                putLong("electric_id", id)
                putLong("electric_progress_time", progressTime)
                putLong("electric_mode_id", modeId)
                apply()
            }

            bundleOf(
                ABridgeContract.KEY_OK to true,
                ABridgeContract.KEY_STATUS to "accepted"
            )
        } catch (e: Exception) {
            Log.e(TAG, "IPC Provider: Failed to parse or store electric result payload. Payload: $payload, Error: ${e.message}", e) // 로그 메시지 상세화
            errorBundle("Failed to process electric result: ${e.message}")
        }
    }

    /** 결과를 저장하고 비동기 워크를 큐에 넣는다. */
    private fun handleResultFromB(requestId: String, payload: String): Boolean {
        val saved = repository.saveResult(requestId, payload)
        if (!saved) return false

        enqueueResultSyncWork(requestId, payload)
        return true
    }

    /** WorkManager에 payload 동기화 작업을 등록한다. */
    private fun enqueueResultSyncWork(requestId: String, payload: String) {
        val workRequest = OneTimeWorkRequestBuilder<BridgeResultSyncWorker>()
            .setInputData(
                workDataOf(
                    ABridgeContract.EXTRA_REQUEST_ID to requestId,
                    ABridgeContract.EXTRA_PAYLOAD to payload
                )
            )
            .build()
        WorkManager.getInstance(appContext).enqueue(workRequest)
    }

    /** 호출자 패키지/서명을 검증하여 allowlist 외 접근을 차단한다. */
    private fun enforceCallerAllowed(context: Context) {
        val uid = Binder.getCallingUid()
        if (uid == android.os.Process.myUid()) return

        val pm = context.packageManager
        val packages = pm.getPackagesForUid(uid)?.toSet().orEmpty()
        if (packages.isEmpty()) {
            throw SecurityException("No packages for uid=${'$'}uid")
        }

        val normalizedAllowedFingerprint = normalizeFingerprint(ALLOWED_CERT_SHA256)
        val callerAllowed = packages.any { packageName ->
            if (packageName != ALLOWED_PACKAGE_NAME) {
                return@any false
            }
            val fingerprints = getFingerprintsForPackage(pm, packageName)
            fingerprints.any { it == normalizedAllowedFingerprint }
        }

        if (!callerAllowed) {
            throw SecurityException("Caller rejected: uid=${'$'}uid packages=${'$'}{packages.joinToString()}")
        }
    }

    /** 패키지의 서명/히스토리를 읽어 SHA-256 지문 세트를 만든다. */
    private fun getFingerprintsForPackage(pm: PackageManager, packageName: String): Set<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                val info = pm.getPackageInfo(packageName, flags)
                extractFingerprints(info)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                extractFingerprints(info)
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.map { it.toSha256Fingerprint() }?.toSet().orEmpty()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found while verifying caller: ${'$'}packageName", e)
            emptySet()
        }
    }

    /** PackageInfo에서 현재/히스토리 서명 지문을 추출한다. */
    private fun extractFingerprints(info: PackageInfo): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptySet()
        val signingInfo = info.signingInfo ?: return emptySet()
        val signatures = mutableSetOf<Signature>()
        signingInfo.apkContentsSigners?.let { signatures.addAll(it) }
        signingInfo.signingCertificateHistory?.let { signatures.addAll(it) }
        return signatures.map { it.toSha256Fingerprint() }.toSet()
    }

    /** Signature를 SHA-256 fingerprint(콜론 제거 소문자)로 변환한다. */
    private fun Signature.toSha256Fingerprint(): String {
        val digest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("Unable to verify signature", e)
        }
        val hash = digest.digest(toByteArray())
        return hash.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    /** fingerprint 문자열을 비교 가능한 형태로 정규화한다. */
    private fun normalizeFingerprint(fingerprint: String): String {
        return fingerprint.replace(":", "").lowercase(Locale.US)
    }

    /** 실패 응답 Bundle 생성 도우미 */
    private fun errorBundle(message: String?): Bundle {
        return bundleOf(
            ABridgeContract.KEY_OK to false,
            ABridgeContract.KEY_ERROR to (message ?: "unknown error")
        )
    }

    // 이하 기본 CRUD는 사용하지 않으므로 빈 구현
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    companion object {
        /** Log 태그 */
        const val TAG: String = "BridgeProvider"
        /** 접근 허용 패키지명 */
        const val ALLOWED_PACKAGE_NAME: String = "com.exosystems.b2b.integration"
        /** 접근 허용 서명 SHA-256 지문(콜론 제거 소문자) */
        const val ALLOWED_CERT_SHA256: String = "6E:0A:49:3E:4C:7E:F0:38:41:A4:17:F1:49:BD:78:22:F7:A2:60:D2:EE:93:32:1F:AB:75:85:88:12:3F:94:38"
    }
}
