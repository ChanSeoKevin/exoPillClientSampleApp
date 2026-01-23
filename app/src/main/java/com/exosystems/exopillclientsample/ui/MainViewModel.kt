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
data class ElectricResultPrefs(
    val userId: Int,
    val dateTime: String,
    val mode: String,
    val amplitude: Int,
    val frequency: Int,
    val period: Double,
    val cycle: Int,
    val phase: Int,
    val onTime: Int,
    val offTime: Int,
    val rampTime: Double,
    val duty: Int,
    val totalTime: Int,
    val progressTime: Int,
    val isBillable: Boolean
)


/**
 * MainActivity의 UI 상태
 * @property selectedPatient 현재 선택된 환자 정보
 * @property electricResult 최근 수신된 전기자극 결과
 */
data class MainUiState(
    val selectedPatient: Patient? = null,
    val electricResult: ElectricResultPrefs? = null
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
        if (sharedPreferences == electricResultSharedPreferences) {
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
        val userId = electricResultSharedPreferences.getInt("userId", Int.MIN_VALUE)
        val dateTime = electricResultSharedPreferences.getString("dateTime", null)
        val mode = electricResultSharedPreferences.getString("mode", null)
        val amplitude = electricResultSharedPreferences.getInt("amplitude", Int.MIN_VALUE)
        val frequency = electricResultSharedPreferences.getInt("frequency", Int.MIN_VALUE)
        val period = electricResultSharedPreferences.getString("period", null)?.toDoubleOrNull()
        val cycle = electricResultSharedPreferences.getInt("cycle", Int.MIN_VALUE)
        val phase = electricResultSharedPreferences.getInt("phase", Int.MIN_VALUE)
        val onTime = electricResultSharedPreferences.getInt("onTime", Int.MIN_VALUE)
        val offTime = electricResultSharedPreferences.getInt("offTime", Int.MIN_VALUE)
        val rampTime = electricResultSharedPreferences.getString("rampTime", null)?.toDoubleOrNull()
        val duty = electricResultSharedPreferences.getInt("duty", Int.MIN_VALUE)
        val totalTime = electricResultSharedPreferences.getInt("totalTime", Int.MIN_VALUE)
        val progressTime = electricResultSharedPreferences.getInt("progressTime", Int.MIN_VALUE)
        val isBillable = electricResultSharedPreferences.getBoolean("isBillable", false)

        val hasAllFields = userId != Int.MIN_VALUE &&
            dateTime != null &&
            mode != null &&
            amplitude != Int.MIN_VALUE &&
            frequency != Int.MIN_VALUE &&
            period != null &&
            cycle != Int.MIN_VALUE &&
            phase != Int.MIN_VALUE &&
            onTime != Int.MIN_VALUE &&
            offTime != Int.MIN_VALUE &&
            rampTime != null &&
            duty != Int.MIN_VALUE &&
            totalTime != Int.MIN_VALUE &&
            progressTime != Int.MIN_VALUE

        if (hasAllFields) {
            _uiState.update { currentState ->
                currentState.copy(
                    electricResult = ElectricResultPrefs(
                        userId = userId,
                        dateTime = dateTime ?: "",
                        mode = mode ?: "",
                        amplitude = amplitude,
                        frequency = frequency,
                        period = period ?: 0.0,
                        cycle = cycle,
                        phase = phase,
                        onTime = onTime,
                        offTime = offTime,
                        rampTime = rampTime ?: 0.0,
                        duty = duty,
                        totalTime = totalTime,
                        progressTime = progressTime,
                        isBillable = isBillable
                    )
                )
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
            remove("userId")
            remove("dateTime")
            remove("mode")
            remove("amplitude")
            remove("frequency")
            remove("period")
            remove("cycle")
            remove("phase")
            remove("onTime")
            remove("offTime")
            remove("rampTime")
            remove("duty")
            remove("totalTime")
            remove("progressTime")
            remove("isBillable")
            remove("electric_progress_time")
            remove("electric_id")
            remove("electric_mode_id")
            apply()
        }
    }
}
