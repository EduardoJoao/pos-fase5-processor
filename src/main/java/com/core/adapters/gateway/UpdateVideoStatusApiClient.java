package com.core.adapters.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateVideoStatusApiClient {
    
    private final RestTemplate restTemplate;

    @Value("${core.api.url}")
    private String coreServiceUrl;
    
    public void execute(String clientId, String videoId, String status, String errorMessage, String zipKey, String size) {
        try {
            String url = coreServiceUrl + "/videos/" + videoId + "/status";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("status", status);
            requestBody.put("videoZipKey", zipKey);
            requestBody.put("videoZipKeySize", size);
            requestBody.put("errorMessage", errorMessage != null ? errorMessage : "");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("idClient", clientId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (Exception e) {
            System.err.println("Erro ao atualizar status do vídeo: " + e.getMessage());
        }
    }
}