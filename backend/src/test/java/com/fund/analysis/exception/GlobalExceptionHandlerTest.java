package com.fund.analysis.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsBadRequestToStandardResult() throws Exception {
        mockMvc.perform(get("/bad-request").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误"));
    }

    @Test
    void mapsExternalApiErrorToStandardResult() throws Exception {
        mockMvc.perform(get("/external-api").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("第三方错误"));
    }

    @Test
    void mapsRuntimeErrorToStandardResult() throws Exception {
        mockMvc.perform(get("/runtime").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    @RestController
    static class TestController {

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("参数错误");
        }

        @GetMapping("/external-api")
        void externalApi() {
            throw new ExternalApiException("第三方错误");
        }

        @GetMapping("/runtime")
        void runtime() {
            throw new RuntimeException("运行时错误");
        }
    }
}
