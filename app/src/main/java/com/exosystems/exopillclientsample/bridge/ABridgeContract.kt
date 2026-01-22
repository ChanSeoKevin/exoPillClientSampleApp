package com.exosystems.exopillclientsample.bridge

import android.net.Uri

/**
 * exoPillClientSample(A)와 B 앱 간 ContentProvider IPC 규격을 정의하는 계약 클래스.
 * authority, method 이름, Bundle 키 등 상호 합의된 상수만 포함한다.
 */
object ABridgeContract {
    /** Provider authority (Manifest와 동일해야 함) */
    const val AUTHORITY: String = "com.exosystems.exopillclientsample.bridge"

    /** Base URI (ContentResolver.call 사용 시 권장) */
    val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

    /** B -> A: 데이터 요청 메서드 이름 */
    const val METHOD_REQUEST_DATA: String = "METHOD_REQUEST_DATA"

    /** B -> A: 처리 결과 제출 메서드 이름 */
    const val METHOD_SUBMIT_RESULT: String = "METHOD_SUBMIT_RESULT"

    /** 요청을 식별하기 위한 선택적 requestId */
    const val EXTRA_REQUEST_ID: String = "EXTRA_REQUEST_ID"

    /** 결과 제출 시 필수 payload(JSON) */
    const val EXTRA_PAYLOAD: String = "EXTRA_PAYLOAD"

    /** 응답 성공 여부 플래그 */
    const val KEY_OK: String = "ok"

    /** request_data 성공 시 반환되는 JSON 문자열 */
    const val KEY_DATA: String = "data"

    /** 실패 시 에러 메시지 */
    const val KEY_ERROR: String = "error"

    /** submit_result 성공 시 추가 상태 텍스트(옵션) */
    const val KEY_STATUS: String = "status"
}
