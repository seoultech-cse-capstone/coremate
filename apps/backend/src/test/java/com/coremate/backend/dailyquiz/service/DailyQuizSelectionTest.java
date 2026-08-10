package com.coremate.backend.dailyquiz.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/**
 * 오늘의 퀴즈 출제 선별 로직 테스트.
 * 후보 풀 앞부분만 잘라 쓰던 기존 구현은 questionId가 빠른 한 과목에서만 출제되고
 * 어제와 6문항 중 5문항이 겹쳤다. 그 회귀를 막는 것이 목적이다.
 */
class DailyQuizSelectionTest {

    private static final int COUNT = 6;

    // 후보 900개 = 과목 6개가 각 150개씩 questionId 구간을 나눠 가진 상태를 가정한다.
    private static final List<Long> CANDIDATE_IDS = LongStream.rangeClosed(1, 900).boxed().toList();

    @Test
    void 같은_날짜는_항상_같은_문항을_출제한다() {
        LocalDate date = LocalDate.of(2026, 8, 2);

        List<Long> first = DailyQuizService.selectDailyQuizQuestionIds(CANDIDATE_IDS, date, COUNT);
        List<Long> second = DailyQuizService.selectDailyQuizQuestionIds(CANDIDATE_IDS, date, COUNT);

        assertThat(first).hasSize(COUNT).containsExactlyElementsOf(second);
    }

    @Test
    void 한_사이클_안에서는_날짜끼리_문항이_겹치지_않는다() {
        // 후보 900개 / 하루 6문항 = 150일이 한 사이클이다.
        int blocksPerCycle = CANDIDATE_IDS.size() / COUNT;
        LocalDate cycleStart = LocalDate.ofEpochDay(
                Math.floorDiv(LocalDate.of(2026, 8, 2).toEpochDay(), blocksPerCycle) * (long) blocksPerCycle
        );

        Set<Long> everSelected = new HashSet<>();
        for (int i = 0; i < blocksPerCycle; i++) {
            List<Long> selected =
                    DailyQuizService.selectDailyQuizQuestionIds(CANDIDATE_IDS, cycleStart.plusDays(i), COUNT);

            assertThat(everSelected).doesNotContainAnyElementsOf(selected);
            everSelected.addAll(selected);
        }

        // 사이클이 끝나면 후보를 정확히 한 번씩 모두 소진한다.
        assertThat(everSelected).containsExactlyInAnyOrderElementsOf(CANDIDATE_IDS);
    }

    @Test
    void 하루치_문항은_서로_중복되지_않는다() {
        LocalDate start = LocalDate.of(2026, 8, 2);

        for (int i = 0; i < 365; i++) {
            List<Long> selected = DailyQuizService.selectDailyQuizQuestionIds(CANDIDATE_IDS, start.plusDays(i), COUNT);

            assertThat(selected).hasSize(COUNT).doesNotHaveDuplicates();
        }
    }

    @Test
    void 후보_풀_전체에서_출제된다() {
        LocalDate start = LocalDate.of(2026, 8, 2);
        Set<Long> everSelected = new HashSet<>();

        for (int i = 0; i < 365; i++) {
            everSelected.addAll(DailyQuizService.selectDailyQuizQuestionIds(CANDIDATE_IDS, start.plusDays(i), COUNT));
        }

        // 기존 구현은 questionId 상위 100개 안에서만 출제했다. 이제 뒤쪽 구간에서도 나와야 한다.
        assertThat(everSelected).anyMatch(id -> id > 100);
        assertThat(everSelected).anyMatch(id -> id > 750);
        assertThat(everSelected).containsExactlyInAnyOrderElementsOf(CANDIDATE_IDS);
    }

    @Test
    void 후보가_출제수보다_적으면_있는_만큼만_출제한다() {
        List<Long> fewCandidates = List.of(1L, 2L, 3L);

        List<Long> selected =
                DailyQuizService.selectDailyQuizQuestionIds(fewCandidates, LocalDate.of(2026, 8, 2), COUNT);

        assertThat(selected).hasSize(3).containsExactlyInAnyOrderElementsOf(fewCandidates);
    }
}
