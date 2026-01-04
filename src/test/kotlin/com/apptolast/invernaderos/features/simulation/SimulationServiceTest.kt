package com.apptolast.invernaderos.features.simulation

import com.apptolast.invernaderos.features.greenhouse.RealDataDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.SimpMessagingTemplate

@ExtendWith(MockitoExtension::class)
class SimulationServiceTest {

    @Mock lateinit var messagingTemplate: SimpMessagingTemplate

    @InjectMocks lateinit var simulationService: SimulationService

    @Captor lateinit var messageCaptor: ArgumentCaptor<RealDataDto>

    @Test
    fun `startSimulation should add tenant to active simulations`() {
        val tenantId = "TEST-TENANT"
        simulationService.startSimulation(tenantId)

        val status = simulationService.getSimulationStatus(tenantId)
        assertTrue(status.isActive)
        assertEquals("CALM", status.phase) // Starts at minute 0 -> CALM
    }

    @Test
    fun `stopSimulation should remove tenant`() {
        val tenantId = "TEST-TENANT-STOP"
        simulationService.startSimulation(tenantId)
        simulationService.stopSimulation(tenantId)

        val status = simulationService.getSimulationStatus(tenantId)
        assertFalse(status.isActive)
    }

    @Test
    fun `tick should send websocket message`() {
        val tenantId = "dcd321ae-8c90-488b-a01c-1c5fa276e001" // Valid UUID string
        simulationService.startSimulation(tenantId)

        simulationService.tick()

        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/greenhouse/messages"), any(RealDataDto::class.java))
    }
}
