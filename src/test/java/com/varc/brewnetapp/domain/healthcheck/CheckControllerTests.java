package com.varc.brewnetapp.domain.healthcheck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("서버 상태 확인")
    @Test
    void healthCheckTest() throws Exception {
        mockMvc.perform(get("/api/v1/check"))
                .andExpect(status().isOk())
                .andExpect(content().string("Server is working good!"))
                .andDo(print());
    }

    @DisplayName("데이터베이스 회사정보 확인")
    @Test
    void checkDBCompanyNameTest() throws Exception {
        mockMvc.perform(get("/api/v1/check/company"))
                .andExpect(status().isOk())
                .andExpect(content().string("메가 커피"))
//                .andExpect(content().string("Alpha Corp"))
                .andDo(print());
    }
}