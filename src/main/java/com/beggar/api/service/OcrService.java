package com.beggar.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final ImageAnnotatorClient imageAnnotatorClient;
    private final ObjectMapper objectMapper;

    @Value("${groq.api-key}")
    private String groqApiKey;

    public String detectText(String imageUrl) {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setImageUri(imageUrl).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        return process(img);
    }

    public String detectTextFromBytes(byte[] imageBytes) {
        Image img = Image.newBuilder().setContent(com.google.protobuf.ByteString.copyFrom(imageBytes)).build();
        return process(img);
    }

    private String process(Image img) {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        try {
            BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    log.error("Error: {}", res.getError().getMessage());
                    return null;
                }
                TextAnnotation annotation = res.getFullTextAnnotation();
                if (annotation != null) {
                    return annotation.getText();
                }
            }
        } catch (Exception e) {
            log.error("OCR detection failed", e);
        }
        return null;
    }

    public Map<String, Object> analyzeWithGroq(String text) {
        String prompt = String.format("""
                당신은 영수증 분석 전문가입니다. 아래 텍스트를 분석하여 오직 JSON 형식으로만 응답하세요.
                JSON에는 반드시 아래의 모든 필드를 포함해야 합니다.

                [카테고리 분류 규칙]
                - 파리바게트, 뚜레쥬르, 베이커리 등은 무조건 '기타 요식업'으로 분류한다. (절대 양식으로 분류하지 말 것)
                - 한식, 양식, 중식, 일식에 해당하면 해당 명칭으로 분류한다.
                - 그외는 '기타 요식업'으로 분류한다.
                
                텍스트: %s

                JSON 형식:
                {
                  "store_name": "상호명",
                  "address": "주소",
                  "total_amount": 0,
                  "date": "YYYY-MM-DD HH:MM:SS",
                  "category": "한식|양식|중식|일식|기타 요식업 중 택 1",
                  "items": [
                    { "name": "상품명", "price": 0, "quantity": 0, "amount": 0 }
                  ]
                }
                """, text);

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .build();

        Map<String, Object> requestBody = Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", "llama-3.3-70b-versatile",
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            log.error("Groq analysis failed", e);
            return null;
        }
    }
}
