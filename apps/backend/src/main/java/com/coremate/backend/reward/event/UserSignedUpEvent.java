package com.coremate.backend.reward.event;

/**
 * 회원가입이 커밋된 뒤에 발행되는 이벤트.
 *
 * <p>가입 직후 지급하는 보상(스타터팩)을 가입 트랜잭션 밖에서 처리하기 위한 신호다.
 */
public record UserSignedUpEvent(Long userId) {
}
