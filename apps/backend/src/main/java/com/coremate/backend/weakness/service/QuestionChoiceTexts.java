package com.coremate.backend.weakness.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 문항의 {@code choices_json}(예: {@code [{"key":"A","text":"..."}]})을 선지 키 → 문장 맵으로 읽는다.
 * 분석표가 "선택: B" 대신 고른 선지의 문장을 보여 주기 위해 쓴다(SCR-WEAK-05).
 */
@Slf4j
final class QuestionChoiceTexts {

    private QuestionChoiceTexts() {
    }

    static Map<String, String> parse(String choicesJson, ObjectMapper objectMapper) {
        if (choicesJson == null || choicesJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> texts = new LinkedHashMap<>();
            for (Choice choice : objectMapper.readValue(choicesJson, new TypeReference<List<Choice>>() {})) {
                if (choice.key() != null && choice.text() != null) {
                    texts.put(choice.key(), choice.text());
                }
            }
            return texts;
        } catch (RuntimeException ex) {
            // 선지 문장이 없으면 기호만 보여 주면 되므로 분석표 전체를 실패시키지 않는다.
            log.warn("문항 선지 JSON 파싱 실패로 선지 문장을 비웁니다. 값 앞부분: {}",
                    choicesJson.substring(0, Math.min(choicesJson.length(), 80)), ex);
            return Map.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(@JsonProperty("key") String key, @JsonProperty("text") String text) {
    }
}
