package com.apptolast.invernaderos.features.simulation

/** Defines the behavior of the simulation over time. The "10-Minute Storm" scenario. */
enum class SimulationPhase(val startMinute: Int, val endMinute: Int, val description: String) {
    CALM(0, 2, "Normal operation. Temperature fluctuates slightly around 22°C."),
    HEATING(2, 4, "Problem detected. Temperature rises rapidly."),
    CRITICAL(4, 5, "Critical state. Temperature exceeds thresholds. Alerts should trigger."),
    ACTION_WAIT(5, 6, "Waiting for actuator intervention. High temperature persists."),
    RECOVERY(6, 8, "Recovery. If ventilated, temperature drops. Otherwise, stays high."),
    NORMALIZATION(8, 10, "Return to normal. Temperature stabilizes.");

    companion object {
        fun fromMinute(minute: Int): SimulationPhase {
            return entries.find { minute in it.startMinute until it.endMinute } ?: NORMALIZATION
        }
    }
}

data class SimulatedValues(
        val temperature: Double,
        val humidity: Double,
        val phase: SimulationPhase,
        val isAlertCondition: Boolean
)
