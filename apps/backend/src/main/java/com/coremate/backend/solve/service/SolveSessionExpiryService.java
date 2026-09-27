package com.coremate.backend.solve.service;

import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제한 시간이 지난 회차의 중단 처리를 담당한다.
 *
 * <p>만료 판정은 제출/완료 요청을 거부하면서 함께 일어나는데, 거부는 예외로 처리되어
 * 호출한 트랜잭션이 롤백된다. 중단 상태만은 남겨야 하므로 REQUIRES_NEW로 분리한다.
 */
@Service
@RequiredArgsConstructor
public class SolveSessionExpiryService {

    private final SolveSessionRepository solveSessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void abandon(Long solveSessionId) {
        solveSessionRepository.findById(solveSessionId).ifPresent(SolveSession::abandon);
    }
}
