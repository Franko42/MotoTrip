package com.mototrip.domain.repository

import com.mototrip.domain.model.MotorcycleConfig
import kotlinx.coroutines.flow.StateFlow

interface MotorcycleConfigRepository {
    val config: StateFlow<MotorcycleConfig>
    suspend fun updateConfig(config: MotorcycleConfig): Result<Unit>
}
