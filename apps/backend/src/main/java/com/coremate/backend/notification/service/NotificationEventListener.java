package com.coremate.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 알림을 유발한 동작이 커밋된 뒤에 발송한다.
//
// 같은 트랜잭션에서 바로 보내면 두 가지가 문제다. 발송 중 DB 오류가 나면 잡아서 삼켜도
// 트랜잭션이 rollback-only 로 표시돼 원래 동작까지 실패한다. 반대로 원래 동작이 나중에
// 롤백되면 일어나지 않은 일에 대한 알림만 남는다.
//
// 커밋 이후 별도 트랜잭션에서 처리하면 두 경우 모두 사라진다. 도메인 서비스는 이벤트만
// 발행하면 되고 알림 모듈을 직접 알 필요도 없다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSender notificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationCommand command) {
        notificationSender.send(command);
    }
}
