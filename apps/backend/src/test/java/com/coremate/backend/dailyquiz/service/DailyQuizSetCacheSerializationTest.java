package com.coremate.backend.dailyquiz.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.dailyquiz.dto.response.DailyQuizQuestionResponse;
import com.coremate.backend.dailyquiz.dto.response.DailyQuizSetResponse;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.problemset.dto.type.QuestionFormat;
import com.coremate.backend.problemset.dto.type.QuestionType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 오늘의 퀴즈 전역 세트를 Redis 에 JSON 으로 캐싱하려면 응답 DTO 가
 * ObjectMapper 로 직렬화 후 다시 역직렬화(round-trip)돼야 한다.
 * 특히 QuestionType 은 @JsonValue("definition" 등)로 직렬화되므로, 캐시에서 읽을 때
 * 그 값이 다시 enum 으로 복원되는지 검증한다(값 손상은 조회 응답을 깨뜨린다).
 */
class DailyQuizSetCacheSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void 오늘의_퀴즈_세트는_캐시_직렬화_후_동일하게_복원된다() {
        DailyQuizSetResponse original = new DailyQuizSetResponse(
                2026092701L,
                LocalDate.of(2026, 9, 27),
                2,
                false,
                List.of(
                        new DailyQuizQuestionResponse(
                                5001L, 1L, "자료구조", 1001L, "완전 이진 트리", 1,
                                QuestionFormat.MCQ_4, QuestionType.DEFINITION,
                                "완전 이진 트리의 정의는?",
                                List.of(new QuestionChoiceResponse("A", "마지막 레벨을 제외하고 모두 채워진다."),
                                        new QuestionChoiceResponse("B", "항상 균형이 맞다.")),
                                2, 45, true
                        ),
                        new DailyQuizQuestionResponse(
                                5002L, 2L, "네트워크", 1002L, "TCP", 2,
                                QuestionFormat.OX, QuestionType.APPLICATION,
                                "TCP 는 연결 지향이다.",
                                List.of(new QuestionChoiceResponse("O", "참"),
                                        new QuestionChoiceResponse("X", "거짓")),
                                1, 20, false
                        )
                )
        );

        String json = objectMapper.writeValueAsString(original);
        DailyQuizSetResponse restored = objectMapper.readValue(json, DailyQuizSetResponse.class);

        assertThat(restored).isEqualTo(original);
    }
}
