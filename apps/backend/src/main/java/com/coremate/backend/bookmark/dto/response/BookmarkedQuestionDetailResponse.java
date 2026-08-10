package com.coremate.backend.bookmark.dto.response;

import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "북마크한 문제 상세 응답 (본문·선택지·정답·해설 포함)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkedQuestionDetailResponse {
    @Schema(example = "10")
    private Long bookmarkId;

    @Schema(example = "101")
    private Long questionId;

    @Schema(example = "55")
    private Long conceptId;

    @Schema(example = "완전 이진 트리")
    private String conceptName;

    @Schema(example = "MCQ_4")
    private String questionFormat;

    @Schema(example = "SINGLE")
    private String questionType;

    @Schema(example = "3")
    private Integer difficultyLevel;

    @Schema(description = "문제 본문")
    private String questionText;

    @Schema(description = "선택지 목록")
    private List<QuestionChoiceResponse> choices;

    @Schema(description = "정답", example = "B")
    private String correctAnswer;

    @Schema(description = "해설")
    private String explanationText;

    @Schema(example = "90")
    private Integer expectedSolveSec;

    @Schema(example = "Review this later")
    private String memo;
}
