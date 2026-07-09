package com.coremate.backend.solve.service;

import com.coremate.backend.content.entity.CommonMistake;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.dto.response.QuestionHintResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionHintService {

    private final QuestionRepository questionRepository;

    /**
     * 문제에 연결된 common_mistake.correction_text 를 힌트로 반환한다.
     * 힌트가 없거나 비활성 상태면 QUESTION_HINT_NOT_FOUND 예외를 던진다.
     * 정답을 노출하지 않기 위해 correct_answer 는 응답에 포함하지 않는다.
     */
    public QuestionHintResponse getQuestionHint(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.QUESTION_NOT_FOUND));

        if (!question.isActive()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }

        CommonMistake hint = question.getHintCommonMistake();
        if (hint == null || !hint.isActive()) {
            throw new CoreMateException(ErrorCode.QUESTION_HINT_NOT_FOUND);
        }

        return new QuestionHintResponse(
                question.getQuestionId(),
                hint.getCommonMistakeId(),
                hint.getCorrectionText()
        );
    }
}
