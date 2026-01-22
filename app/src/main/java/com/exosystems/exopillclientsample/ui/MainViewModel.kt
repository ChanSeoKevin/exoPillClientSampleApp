package com.exosystems.exopillclientsample.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 환자 정보 데이터 클래스
 * @property patientId 환자 ID
 * @property hospitalName 병원 이름
 */
data class Patient(
    val patientId: String,
    val hospitalName: String
)

/**
 * 전기자극 결과 데이터 클래스
 * @property id 결과 ID
 * @property progressTime 진행 시간 (밀리초)
 * @property modeId 모드 ID
 */
data class ElectricResult(
    val id: Long,
    val progressTime: Long,
    val modeId: Long
)


/**
 * MainActivity의 UI 상태
 * @property selectedPatient 현재 선택된 환자 정보
 * @property electricResult 최근 수신된 전기자극 결과
 */
data class MainUiState(
    val selectedPatient: Patient? = null,
    val electricResult: ElectricResult? = null
)

/**
 * MainActivity의 비즈니스 로직과 상태를 관리하는 ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val patientSharedPreferences = application.getSharedPreferences("patient_prefs", Context.MODE_PRIVATE)
    private val electricResultSharedPreferences = application.getSharedPreferences("electric_result_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadPatient()
        loadElectricResult()
        patientSharedPreferences.registerOnSharedPreferenceChangeListener(this)
        electricResultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        patientSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        electricResultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == electricResultSharedPreferences && (key == "electric_id" || key == "electric_progress_time" || key == "electric_mode_id")) {
            loadElectricResult()
        }
        if (sharedPreferences == patientSharedPreferences && (key == "patient_id" || key == "hospital_name")) {
            loadPatient()
        }
    }

    private fun loadPatient() {
        val patientId = patientSharedPreferences.getString("patient_id", null)
        val hospitalName = patientSharedPreferences.getString("hospital_name", null)

        if (patientId != null && hospitalName != null) {
            _uiState.update { currentState ->
                currentState.copy(selectedPatient = Patient(patientId, hospitalName))
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(selectedPatient = null)
            }
        }
    }

    private fun loadElectricResult() {
        val id = electricResultSharedPreferences.getLong("electric_id", -1L)
        val progressTime = electricResultSharedPreferences.getLong("electric_progress_time", -1L)
        val modeId = electricResultSharedPreferences.getLong("electric_mode_id", -1L)

        if (id != -1L && progressTime != -1L && modeId != -1L) {
            _uiState.update { currentState ->
                currentState.copy(electricResult = ElectricResult(id, progressTime, modeId))
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(electricResult = null)
            }
        }
    }

    /**
     * 환자를 선택하고 상태를 업데이트 및 저장합니다.
     */
    fun selectPatient() {
        val randomId = (1000..9999).random()
        val newPatient = Patient(
            patientId = "P$randomId",
            hospitalName = "Exo Hospital"
        )
        _uiState.update { it.copy(selectedPatient = newPatient) }
        with(patientSharedPreferences.edit()) {
            putString("patient_id", newPatient.patientId)
            putString("hospital_name", newPatient.hospitalName)
            apply()
        }
    }

    /**
     * 선택된 환자 정보를 초기화 및 삭제합니다.
     */
    fun clearPatient() {
        _uiState.update { it.copy(selectedPatient = null) }
        with(patientSharedPreferences.edit()) {
            remove("patient_id")
            remove("hospital_name")
            apply()
        }
    }

    /**
     * 선택된 전기자극 결과 정보를 초기화 및 삭제합니다.
     */
    fun clearElectricResult() {
        _uiState.update { it.copy(electricResult = null) }
        with(electricResultSharedPreferences.edit()) {
            remove("electric_id")
            remove("electric_progress_time")
            remove("electric_mode_id")
            apply()
        }
    }
}
