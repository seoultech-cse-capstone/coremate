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

    /** 약점분석 1건 생성 시 차감 코인(플레이스홀더 — 확정 가격표 없음). */
    private int coinCost = 100;

    /** AI 서버(Python ai-pipeline) 기본 URL. */
    private String aiServerBaseUrl = "http://localhost:8000";

    /**
     * AI 호출 읽기 타임아웃(초).
     *
     * AI 서버가 추론 모델을 쓰기 때문에 한 번 호출이 30초 안팎 걸린다. 실제 호출 기록(ai_call_logs)이
     * 26.8s / 31.6s / 32.7s / 35.8s 라 기존 40초는 여유가 5초도 안 됐고, 실제로 한 번 타임아웃으로
     * 생성이 실패했다. 실패하면 트랜잭션 롤백으로 코인은 돌아오지만 사용자는 오류 화면만 보게 된다.
     */
    private int aiTimeoutSec = 120;

    /** 교차검증 시 유형 구분도가 이 값 미만이면 AI 유형 채택, 이상이면 AMBIGUOUS. */
    private double typeSeparationThreshold = 0.3;

    /** 요청 지문·호출 로그에 남길 버전 라벨. */
    private String scoringRuleVersion = "v1";
    /** v2: 진단 프로필 신호(skill_type·misconception_code·repeated_misconceptions) 활용 지침 추가. */
    private String promptVersion = "v3";
    private String schemaVersion = "v1";
    /**
     * 사용할 진단 프로필 데이터 버전. 이 버전으로 승인(APPROVED)된 프로필만 AI 입력·리포트에 결합한다.
     * {@code none} 이면 프로필을 전혀 읽지 않는다(프로필 도입 전과 동일 동작).
     */
    private String diagnosticProfileVersion = "v1";
    private String aiModelLabel = "ai-pipeline";
}
