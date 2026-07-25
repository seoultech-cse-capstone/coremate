package com.coremate.backend.weakness.dto.ai;

import com.coremate.backend.weakness.dto.type.*;
import java.util.List;

// AI 서버가 반환하는 약점 분석 해석.
public record AiWeaknessAnalysisResponse(String overview, List<ConceptAnalysis> conceptAnalyses) {

    public record ConceptAnalysis(Long conceptId, AiAnalysisStatus analysisStatus, WeaknessType primaryType,
            List<SecondarySignal> secondarySignals, CauseCode causeCode, String confusedRelation,
            String diagnosis, String likelyCause, String conceptTip, List<String> evidenceRefs) {}
}
