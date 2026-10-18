package com.mototrip.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mototrip.domain.model.ConsumptionSource
import com.mototrip.domain.model.MotorcycleConfig
import com.mototrip.domain.repository.MotorcycleConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MotorcycleConfigRepositoryImpl(context: Context) : MotorcycleConfigRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("motorcycle_config_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    override val config: StateFlow<MotorcycleConfig> = _config.asStateFlow()

    private fun loadConfig(): MotorcycleConfig {
        val model = prefs.getString("model_name", "Bajaj Boxer BM150") ?: "Bajaj Boxer BM150"
        val capacity = prefs.getFloat("tank_capacity", 11.0f).toDouble()
        val reserve = prefs.getFloat("reserve_liters", 2.5f).toDouble()
        val referenceCons = prefs.getFloat("reference_consumption", 1.90f).toDouble()
        val isCapConf = prefs.getBoolean("is_capacity_confirmed", false)
        val isResConf = prefs.getBoolean("is_reserve_confirmed", false)
        val sourceStr = prefs.getString("consumption_source", ConsumptionSource.MANUAL_REFERENCE.name)
        val source = runCatching { ConsumptionSource.valueOf(sourceStr ?: "") }.getOrDefault(ConsumptionSource.MANUAL_REFERENCE)

        return MotorcycleConfig(
            modelName = model,
            tankCapacityLiters = capacity,
            reserveLiters = reserve,
            referenceConsumptionL100 = referenceCons,
            isCapacityConfirmed = isCapConf,
            isReserveConfirmed = isResConf,
            consumptionSource = source
        )
    }

    override suspend fun updateConfig(config: MotorcycleConfig): Result<Unit> = runCatching {
        prefs.edit()
            .putString("model_name", config.modelName)
            .putFloat("tank_capacity", config.tankCapacityLiters.toFloat())
            .putFloat("reserve_liters", config.reserveLiters.toFloat())
            .putFloat("reference_consumption", config.referenceConsumptionL100.toFloat())
            .putBoolean("is_capacity_confirmed", config.isCapacityConfirmed)
            .putBoolean("is_reserve_confirmed", config.isReserveConfirmed)
            .putString("consumption_source", config.consumptionSource.name)
            .apply()
        _config.value = config
    }
}
