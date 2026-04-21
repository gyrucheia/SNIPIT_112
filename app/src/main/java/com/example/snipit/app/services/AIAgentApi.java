package com.example.snipit.app.services;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AIAgentApi {
    @POST("/analyze")
    Call<AgentResponse> analyzeCode(@Body CodeRequest request);

    class CodeRequest {
        public String code;
        public String language;

        public CodeRequest(String code, String language) {
            this.code = code;
            this.language = language;
        }
    }

    class AgentResponse {
        public String fixed_code;
        public String tags;
        public String explanation;
    }
}
