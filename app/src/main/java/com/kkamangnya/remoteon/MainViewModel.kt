package com.kkamangnya.remoteon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val pcs: List<RemotePc> = emptyList(),
    val isLoading: Boolean = true
)

class MainViewModel(private val repository: PcRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val pcs = repository.loadPcs()
            _uiState.update { it.copy(pcs = pcs, isLoading = false) }
        }
    }

    fun savePc(pc: RemotePc) {
        viewModelScope.launch {
            repository.upsertPc(pc)
            refresh()
            _events.emit("PC 정보를 저장했습니다.")
        }
    }

    fun deletePc(pc: RemotePc) {
        viewModelScope.launch {
            repository.deletePc(pc.id)
            refresh()
            _events.emit("${pc.name} 항목을 삭제했습니다.")
        }
    }

    fun wakePc(pc: RemotePc) {
        viewModelScope.launch {
            repository.sendWake(pc)
            _events.emit("${pc.name}에 WoL 패킷을 보냈습니다.")
        }
    }

    fun pingPc(pc: RemotePc) {
        viewModelScope.launch {
            val state = repository.checkOnline(pc)
            val checkedAt = System.currentTimeMillis()
            _uiState.update { current ->
                current.copy(
                    pcs = current.pcs.map {
                        if (it.id == pc.id) it.copy(connectionState = state, lastCheckedAt = checkedAt) else it
                    }
                )
            }
            val message = when (state) {
                ConnectionState.Online -> "${pc.name}은 온라인입니다."
                ConnectionState.Offline -> "${pc.name}은 오프라인입니다."
                ConnectionState.Unknown -> "${pc.name} 상태를 확인할 수 없습니다."
            }
            _events.emit(message)
        }
    }
}

