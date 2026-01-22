package com.exosystems.exopillclientsample.ui

import android.app.Application
import android.content.Context
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
 * MainActivity의 UI 상태
 * @property selectedPatient 현재 선택된 환자 정보
 */
data class MainUiState(
    val selectedPatient: Patient? = null
)

/**
 * MainActivity의 비즈니스 로직과 상태를 관리하는 ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("patient_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadPatient()
    }

    private fun loadPatient() {
        val patientId = sharedPreferences.getString("patient_id", null)
        val hospitalName = sharedPreferences.getString("hospital_name", null)

        if (patientId != null && hospitalName != null) {
            _uiState.value = MainUiState(selectedPatient = Patient(patientId, hospitalName))
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
        with(sharedPreferences.edit()) {
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
        with(sharedPreferences.edit()) {
            remove("patient_id")
            remove("hospital_name")
            apply()
        }
    }
}
