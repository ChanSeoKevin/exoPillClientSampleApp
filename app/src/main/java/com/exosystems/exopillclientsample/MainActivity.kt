package com.exosystems.exopillclientsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exosystems.exopillclientsample.ui.MainViewModel
import com.exosystems.exopillclientsample.ui.theme.ExoPillClientSampleTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExoPillClientSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "현재 선택된 환자:")
        Text(text = uiState.selectedPatient?.toString() ?: "없음")

        Text(text = "최근 전기자극 결과:")
        val electricResult = uiState.electricResult
        if (electricResult == null) {
            Text(text = "없음")
        } else {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = "userId: ${electricResult.userId}")
                Text(text = "dateTime: ${electricResult.dateTime}")
                Text(text = "mode: ${electricResult.mode}")
                Text(text = "amplitude: ${electricResult.amplitude}")
                Text(text = "frequency: ${electricResult.frequency}")
                Text(text = "period: ${electricResult.period}")
                Text(text = "cycle: ${electricResult.cycle}")
                Text(text = "phase: ${electricResult.phase}")
                Text(text = "onTime: ${electricResult.onTime}")
                Text(text = "offTime: ${electricResult.offTime}")
                Text(text = "rampTime: ${electricResult.rampTime}")
                Text(text = "duty: ${electricResult.duty}")
                Text(text = "totalTime: ${electricResult.totalTime}")
                Text(text = "progressTime: ${electricResult.progressTime}")
                Text(text = "isBillable: ${electricResult.isBillable}")
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = { viewModel.selectPatient() }) {
                Text(text = "환자 선택")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.clearPatient() }) {
                Text(text = "환자 초기화")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.clearElectricResult() }) {
                Text(text = "전기자극 결과 초기화")
            }
        }
    }
}
