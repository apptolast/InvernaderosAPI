package com.apptolast.invernaderos.features.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LogoutIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 200 OK when logging out`() {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isOk)
    }
}
