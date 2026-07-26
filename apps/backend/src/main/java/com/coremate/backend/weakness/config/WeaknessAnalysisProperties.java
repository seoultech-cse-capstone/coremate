package com.coremate.backend.weakness.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 약점분석 결정론 코어의 튜너블 수치. 문서에 확정값이 없어 플레이스홀더로 시작하며,
 * application.yaml 의 {@code weakness.analysis.*} 로 오버라이드 가능하다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "weakness.analysis")
public class WeaknessAnalysisProperties {

    /** 분석 대상 회차 최소/최대 선택 수. */
    private int minSelection = 1;
    private int maxSelection = 5;

    /** 근거 충분 판정을 위한 최소 문항 수(이 미만이면 근거 부족). */
    private int minQuestionCount = 5;

    /** 약점 후보로 채택할 최소 약점 점수(0~100). */
    private int minWeaknessScore = 60;

    /** 약점 후보로 채택할 최소 근거 충분도(0~1). */
    private double minEvidenceStrength = 0.5;

    /** 약점 후보 최대 개수. */
    private int maxCandidates = 3;

    /** 개념당 대표 근거 최대 개수. */
    private int maxEvidencePerConcept = 3;

    /** 롱타임(오래 걸린 문제) 판정 배수: 기준 시간 * 배수 이상이면 오래 걸린 문제. */
    private double longTimeRatio = 2.0;
}
