package com.coremate.backend.reward.service;

import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 계정 단위로 한 번만 나가는 보상(스타터팩 등).
 *
 * <p><b>가입 트랜잭션이 커밋된 뒤에</b> 지급한다. 이유가 두 가지다.
 * <ul>
 *   <li>보상은 가입보다 부수적이라, 지급이 실패해도 회원가입은 확정되어야 한다.
 *       같은 트랜잭션에서 돌면 호출부가 예외를 삼켜도 Spring 이 트랜잭션을
 *       rollback-only 로 표시해 커밋 시점에 터진다.</li>
 *   <li>단순히 {@code REQUIRES_NEW} 로만 분리하면 새 트랜잭션이 아직 커밋되지 않은
 *       사용자 행을 볼 수 없어 {@code USER_NOT_FOUND} 로 실패한다. 그래서 커밋
 *       이후({@code AFTER_COMMIT})에 실행한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountRewardService {

    private final RewardService rewardService;

    /**
     * 신규 가입 스타터팩. 정책이 {@code PER_USER} + {@code first_reward_only} 라
     * 계정당 1회만 나간다.
     *
     * <p>가입은 이미 커밋된 뒤이므로 여기서 실패해도 가입은 그대로 유지된다.
     * 실패를 삼키고 로그만 남긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void grantStarterPack(UserSignedUpEvent event) {
        try {
            rewardService.grantPolicyReward(
                    event.userId(),
                    RewardType.STARTER_PACK,
                    RewardSource.STARTER_PACK,
                    null,
                    RewardTargetType.USER,
                    event.userId(),
                    null
            );
        } catch (Exception e) {
            log.error("스타터팩 지급 실패. 가입은 유지된다. userId={}", event.userId(), e);
        }
    }
}
