package com.coremate.backend.weakness.dto.ai;

import java.util.List;

// 백엔드가 AI 서버로 보내는 약점 분석 입력(압축 근거).
public record AiWeaknessAnalysisRequest(Scope scope, Totals totals, List<Candidate> weaknessCandidates) {

    public record Scope(String subject) {}

    public record Totals(int questionCount, int wrongCount, int slowCount, int hintUsedCount) {}

    public record Candidate(Long conceptId, String conceptName, String conceptCore, int weaknessScore,
            double evidenceStrength, double typeSeparation, Stats stats,
            List<RepeatedMisconception> repeatedMisconceptions, List<Evidence> evidence) {}

    public record Stats(double wrongRate, double slowRate, double hintRate) {}

    public record RepeatedMisconception(String code, int count) {}

    public record Evidence(String ref, String type, String skillType, String misconceptionCode, Double timeRatio) {}
}
