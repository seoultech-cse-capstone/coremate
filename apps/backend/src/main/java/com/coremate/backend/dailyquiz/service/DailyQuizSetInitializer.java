package com.coremate.backend.dailyquiz.service;

import com.coremate.backend.dailyquiz.entity.DailyQuizSet;
import com.coremate.backend.dailyquiz.repository.DailyQuizSetRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오늘의 퀴즈 세트를 "없으면 만든다"를 여러 요청이 동시에 해도 안전하게 처리한다.
 *
 * <p>앱은 시작할 때 오늘의 퀴즈를 여러 번 조회한다. 날짜가 바뀐 뒤 그날 첫 조회들이 겹치면
 * 모두 "세트 없음"으로 판단해 각자 6행을 넣으려 하고, {@code uq_daily_quiz_sets_date_question}·
 * {@code uq_daily_quiz_sets_date_order} 유니크 제약에서 서로 락을 기다리다 데드락이 난다.
 * 실제로 매일 첫 실행에서 홈 화면이 500 을 받았다.
 *
 * <p>생성을 독립 트랜잭션으로 떼어 내고, 경합에서 진 쪽은 예외를 삼킨다. 이긴 쪽이 이미 같은
 * 세트를 만들어 두었으므로 진 쪽은 그대로 다시 읽기만 하면 된다. 독립 트랜잭션으로 두는 이유는
 * 데드락이 호출한 쪽 트랜잭션까지 롤백 대상으로 만들어, 삼켜도 이어서 쓸 수 없기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQuizSetInitializer {

    private final DailyQuizSetRepository dailyQuizSetRepository;

    /**
     * 아직 없으면 세트를 만든다. 다른 요청이 먼저 만들었으면 조용히 넘어간다.
     *
     * @param entrySupplier 저장할 세트 항목을 만드는 함수. 이미 있으면 호출하지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfAbsent(LocalDate quizDate, Supplier<List<DailyQuizSet>> entrySupplier) {
        if (!dailyQuizSetRepository.findByQuizDateOrderBySortOrderAsc(quizDate).isEmpty()) {
            return;
        }
        try {
            dailyQuizSetRepository.saveAll(entrySupplier.get());
        } catch (DataIntegrityViolationException | CannotAcquireLockException e) {
            // 같은 순간 다른 요청이 먼저 만들었다는 뜻. 호출한 쪽이 다시 읽으면 그 결과를 본다.
            log.debug("오늘의 퀴즈 세트 생성 경합에서 밀렸습니다. 이미 만들어진 세트를 사용합니다. date={}", quizDate);
        }
    }
}
