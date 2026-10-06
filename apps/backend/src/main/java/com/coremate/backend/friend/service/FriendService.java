package com.coremate.backend.friend.service;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.service.AvatarService;
import com.coremate.backend.avatar.service.RoomService;
import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.BlockedUserResponse;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.dto.response.FriendSubjectStatResponse;
import com.coremate.backend.progress.service.ProgressService;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.entity.Friend;
import com.coremate.backend.friend.entity.FriendPoke;
import com.coremate.backend.friend.repository.FriendPokeRepository;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.friend.repository.FriendUserSearchRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
import com.coremate.backend.learninghistory.dto.response.LearningHistoryResponse;
import com.coremate.backend.learninghistory.service.LearningHistoryService;
import com.coremate.backend.notification.service.NotificationMessages;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private static final String REQUEST_DIRECTION_RECEIVED = "RECEIVED";
    private static final String REQUEST_DIRECTION_SENT = "SENT";
    private static final String REQUEST_DIRECTION_ALL = "ALL";

    private static final int POKE_DAILY_LIMIT_PER_FRIEND = 3;
    private static final int POKE_DAILY_LIMIT_TOTAL = 30;

    private final FriendRepository friendRepository;
    private final FriendPokeRepository friendPokeRepository;
    private final FriendUserSearchRepository friendUserSearchRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AvatarService avatarService;
    private final RoomService roomService;
    private final LearningHistoryService learningHistoryService;
    private final ProgressService progressService;
    private final ApplicationEventPublisher eventPublisher;
    private final IdempotencyGuard idempotencyGuard;

    public List<FriendSearchUserResponse> searchUsers(Long userId, String keyword) {
        findActiveUser(userId);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // 친구 추가 화면에서 너무 많은 사용자가 노출되지 않도록 상위 20명만 반환한다.
        List<User> users = friendUserSearchRepository.searchActiveUsers(
                userId,
                keyword.trim(),
                UserStatus.ACTIVE,
                PageRequest.of(0, 20)
        );
        // 목록 아이콘용 장착은 한 번에 읽는다.
        Map<Long, List<AvatarEquippedItemResponse>> equipped = avatarService.getEquippedItemsByUser(
                users.stream().map(User::getUserId).toList());
        List<FriendSearchUserResponse> results = new ArrayList<>();
        for (User user : users) {
            Optional<Friend> relation = findRelation(userId, user.getUserId());
            // 나를 차단한 사람은 검색에 보이지 않는다. 차단 사실은 알리지 않는다.
            if (relation.filter(friend -> isBlockedBy(friend, user.getUserId())).isPresent()) {
                continue;
            }
            results.add(toFriendSearchUserResponse(userId, user, relation,
                    equipped.getOrDefault(user.getUserId(), List.of())));
        }
        return results;
    }

    // 친구 목록 화면은 수락된 친구만 보여준다.
    @Transactional
    public List<FriendResponse> getFriends(Long userId) {
        findActiveUser(userId);
        return friendRepository.findAllByUserIdAndStatus(userId, FriendStatus.ACCEPTED)
                .stream()
                .filter(friend -> friend.getOtherUser(userId).getStatus() == UserStatus.ACTIVE)
                .map(friend -> toFriendResponse(friend, userId))
                .toList();
    }

    public List<FriendRequestResponse> getRequests(Long userId, String direction) {
        findActiveUser(userId);
        String normalizedDirection = normalizeDirection(direction);
        List<Friend> requests = switch (normalizedDirection) {
            case REQUEST_DIRECTION_RECEIVED -> receivedRequests(userId);
            case REQUEST_DIRECTION_SENT -> sentRequests(userId);
            default -> Stream.concat(receivedRequests(userId).stream(), sentRequests(userId).stream())
                    .sorted(Comparator.comparing(Friend::getCreatedAt).reversed())
                    .toList();
        };
        Map<Long, List<AvatarEquippedItemResponse>> equipped = avatarService.getEquippedItemsByUser(
                requests.stream().map(friend -> friend.getRequester().getUserId()).toList());
        return requests.stream()
                .map(friend -> toFriendRequestResponse(friend, userId,
                        equipped.getOrDefault(friend.getRequester().getUserId(), List.of())))
                .toList();
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long userId, SendFriendRequest request) {
        User requester = findActiveUser(userId);
        User receiver = findActiveUser(request.getReceiverId());
        validateNotSelf(requester.getUserId(), receiver.getUserId());

        Long lowId = lowId(requester.getUserId(), receiver.getUserId());
        Long highId = highId(requester.getUserId(), receiver.getUserId());
        Friend friend = friendRepository.findByUserLowIdAndUserHighId(lowId, highId)
                .map(existing -> reopenRequestIfPossible(existing, requester, receiver))
                .orElseGet(() -> friendRepository.save(new Friend(requester, receiver)));

        eventPublisher.publishEvent(NotificationMessages.friendRequestReceived(
                receiver.getUserId(), requester.getNickname(), requester.getUserId()));

        Long requesterId = friend.getRequester().getUserId();
        return toFriendRequestResponse(friend, userId,
                avatarService.getEquippedItemsByUser(List.of(requesterId)).getOrDefault(requesterId, List.of()));
    }

    @Transactional
    public FriendResponse respond(Long userId, RespondFriendRequest request) {
        validateRespondStatus(request.getStatus());
        Friend friend = friendRepository.findByFriendIdAndReceiver_UserId(request.getFriendId(), userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.FRIEND_REQUEST_NOT_RECEIVED));
        if (friend.getStatus() != FriendStatus.REQUESTED) {
            throw new CoreMateException(ErrorCode.FRIEND_REQUEST_NOT_RECEIVED);
        }

        if (request.getStatus() == FriendStatus.ACCEPTED) {
            friend.accept(LocalDateTime.now());
            // 거절은 알리지 않는다(FR-NOTI-08). 거절당한 사실을 통보받는 건 관계에 도움이 되지 않는다.
            eventPublisher.publishEvent(NotificationMessages.friendRequestAccepted(
                    friend.getRequester().getUserId(),
                    friend.getReceiver().getNickname(),
                    friend.getReceiver().getUserId()));
            return toFriendResponse(friend, userId);
        }

        friend.reject();
        return toFriendResponse(friend, userId);
    }

    // 친구 삭제는 수락된 관계만 지운다. 거절 기록까지 지울 수 있으면 거절당한 쪽이 지우고 다시 요청할 수 있다.
    @Transactional
    public void delete(Long userId, Long friendUserId) {
        validateNotSelf(userId, friendUserId);
        Friend friend = findAnyRelation(userId, friendUserId);
        if (!friend.involves(userId) || friend.getStatus() != FriendStatus.ACCEPTED) {
            throw new CoreMateException(ErrorCode.FRIEND_NOT_FOUND);
        }
        friendRepository.delete(friend);
    }

    // 차단. 친구였다면 관계가 끊기고, 상대는 나를 검색하거나 친구 요청을 보낼 수 없다.
    // 프로필·콕 찌르기는 수락된 관계만 되므로 함께 막힌다. 차단 사실은 상대에게 알리지 않는다.
    @Transactional
    public void block(Long userId, Long targetUserId) {
        User blocker = findActiveUser(userId);
        validateNotSelf(userId, targetUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        Optional<Friend> relation = findRelation(userId, targetUserId);
        if (relation.isEmpty()) {
            Friend friend = new Friend(blocker, target);
            friend.block(blocker, target);
            friendRepository.save(friend);
            return;
        }
        // 상대가 이미 나를 차단했으면 그대로 둔다. 내 차단으로 덮어쓰면 상대의 차단이 풀린다.
        if (!isBlockedBy(relation.get(), targetUserId)) {
            relation.get().block(blocker, target);
        }
    }

    // 차단 해제. 관계 자체를 지워 처음 상태로 돌아간다(다시 친구가 되려면 요청부터).
    @Transactional
    public void unblock(Long userId, Long targetUserId) {
        validateNotSelf(userId, targetUserId);
        Friend friend = findRelation(userId, targetUserId)
                .filter(found -> isBlockedBy(found, userId))
                .orElseThrow(() -> new CoreMateException(ErrorCode.FRIEND_BLOCK_NOT_FOUND));
        friendRepository.delete(friend);
    }

    public List<BlockedUserResponse> getBlockedUsers(Long userId) {
        findActiveUser(userId);
        List<Friend> blocks = friendRepository.findByRequester_UserIdAndStatusOrderByUpdatedAtDesc(
                userId, FriendStatus.BLOCKED);
        Map<Long, List<AvatarEquippedItemResponse>> equipped = avatarService.getEquippedItemsByUser(
                blocks.stream().map(friend -> friend.getReceiver().getUserId()).toList());
        return blocks.stream()
                .map(friend -> new BlockedUserResponse(
                        friend.getReceiver().getUserId(),
                        friend.getReceiver().getNickname(),
                        friend.getUpdatedAt(),
                        equipped.getOrDefault(friend.getReceiver().getUserId(), List.of())))
                .toList();
    }

    private boolean isBlockedBy(Friend friend, Long blockerId) {
        return friend.getStatus() == FriendStatus.BLOCKED && friend.getRequester().getUserId().equals(blockerId);
    }

    private Optional<Friend> findRelation(Long userId, Long otherUserId) {
        return friendRepository.findByUserLowIdAndUserHighId(lowId(userId, otherUserId), highId(userId, otherUserId));
    }

    private List<Friend> receivedRequests(Long userId) {
        return friendRepository.findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendStatus.REQUESTED);
    }

    // 보낸 요청에는 거절된 것도 넣는다. 보낸 사람에게는 거절을 알리지 않고 대기 중으로 보여 준다(visibleStatus).
    private List<Friend> sentRequests(Long userId) {
        return friendRepository.findByRequester_UserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(FriendStatus.REQUESTED, FriendStatus.REJECTED));
    }

    @Transactional
    public FriendProfileResponse getProfile(Long userId, Long friendUserId) {
        validateAcceptedFriend(userId, friendUserId);
        User friendUser = findActiveUser(friendUserId);
        return toFriendProfileResponse(friendUser);
    }

    // 친구 상세 프로필 Learning History 섹션. 친구 관계(ACCEPTED)인 경우에만 조회 가능하다. (FR-FRIEND-05)
    // 친구가 학습 기록을 공개하지 않았으면 0으로 채우지 않고 막는다. 0은 "아직 안 했다"로 읽힌다.
    public LearningHistoryResponse getLearningHistory(Long userId, Long friendUserId) {
        validateAcceptedFriend(userId, friendUserId);
        User friendUser = findActiveUser(friendUserId);
        if (!friendUser.isLearningPublic()) {
            throw new CoreMateException(ErrorCode.FRIEND_LEARNING_PRIVATE);
        }
        return learningHistoryService.getStats(friendUserId);
    }

    // 컨트롤러 진입점. 멱등키를 먼저 등록해 연타·재시도로 poke 가 중복 생성/알림되지 않게 막고(#168),
    // 실제 로직은 기존 poke(userId, request) 를 같은 트랜잭션 안에서 재사용한다.
    @Transactional
    public FriendPokeResponse poke(Long userId, PokeFriendRequest request, String idempotencyKey) {
        idempotencyGuard.register(userId, idempotencyKey, "FRIEND_POKE");
        return poke(userId, request);
    }

    @Transactional
    public FriendPokeResponse poke(Long userId, PokeFriendRequest request) {
        validateNotSelf(userId, request.getToUserId());
        validateAcceptedFriend(userId, request.getToUserId());
        validatePokeLimit(userId, request.getToUserId());
        User fromUser = findActiveUser(userId);
        User toUser = findActiveUser(request.getToUserId());

        FriendPoke friendPoke = friendPokeRepository.save(new FriendPoke(fromUser, toUser, request.getMessageType()));
        eventPublisher.publishEvent(NotificationMessages.friendPoke(
                toUser.getUserId(), fromUser.getNickname(), fromUser.getUserId()));
        return toFriendPokeResponse(friendPoke);
    }

    // 콕 찌르기 일일 제한. 컨테이너 타임존이 Asia/Seoul 이라 LocalDate.now() 가 곧 서비스 기준 날짜다.
    // 전체 한도를 먼저 본다. 총량을 다 쓴 상태에서 "같은 친구에게는 3번까지"를 안내하면
    // 다른 친구를 고르면 된다는 뜻으로 읽혀 사용자가 다시 시도하게 된다.
    private void validatePokeLimit(Long fromUserId, Long toUserId) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);

        long sentToday = friendPokeRepository
                .countByFromUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        fromUserId, dayStart, nextDayStart);
        if (sentToday >= POKE_DAILY_LIMIT_TOTAL) {
            throw new CoreMateException(ErrorCode.FRIEND_POKE_DAILY_LIMIT_EXCEEDED);
        }

        long sentToFriendToday = friendPokeRepository
                .countByFromUser_UserIdAndToUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        fromUserId, toUserId, dayStart, nextDayStart);
        if (sentToFriendToday >= POKE_DAILY_LIMIT_PER_FRIEND) {
            throw new CoreMateException(ErrorCode.FRIEND_POKE_FRIEND_LIMIT_EXCEEDED);
        }
    }

    // 거절된 관계는 거절한 쪽(그 요청을 받았던 사람)만 다시 요청할 수 있다. 거절당한 쪽에는 요청이
    // 계속 대기 중인 것처럼 보이므로(거절은 알리지 않는다, FR-FRIEND-04) 그쪽의 재요청은 중복 요청으로 막는다.
    private Friend reopenRequestIfPossible(Friend friend, User requester, User receiver) {
        // 내가 차단한 사람에게는 차단을 풀어야 요청할 수 있다. 나를 차단한 사람은 없는 사용자처럼 다룬다.
        if (friend.getStatus() == FriendStatus.BLOCKED) {
            throw new CoreMateException(isBlockedBy(friend, requester.getUserId())
                    ? ErrorCode.FRIEND_BLOCKED_BY_ME
                    : ErrorCode.USER_NOT_FOUND);
        }
        if (friend.getStatus() == FriendStatus.REJECTED
                && friend.getReceiver().getUserId().equals(requester.getUserId())) {
            friend.reopen(requester, receiver);
            return friend;
        }
        throw new CoreMateException(ErrorCode.FRIEND_REQUEST_DUPLICATED);
    }

    private void validateAcceptedFriend(Long userId, Long friendUserId) {
        validateNotSelf(userId, friendUserId);
        Long lowId = lowId(userId, friendUserId);
        Long highId = highId(userId, friendUserId);
        if (!friendRepository.existsPairByStatus(lowId, highId, FriendStatus.ACCEPTED)) {
            throw new CoreMateException(ErrorCode.FRIEND_RELATION_REQUIRED);
        }
    }

    private Friend findAnyRelation(Long userId, Long friendUserId) {
        return friendRepository.findByUserLowIdAndUserHighId(lowId(userId, friendUserId), highId(userId, friendUserId))
                .orElseThrow(() -> new CoreMateException(ErrorCode.FRIEND_NOT_FOUND));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CoreMateException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return user;
    }

    private FriendResponse toFriendResponse(Friend friend, Long currentUserId) {
        User friendUser = friend.getOtherUser(currentUserId);
        return new FriendResponse(
                friend.getFriendId(),
                friendUser.getUserId(),
                friendUser.getNickname(),
                friendUser.getSchoolName(),
                friendUser.getLevel(),
                friend.getStatus(),
                hasAttendedToday(friendUser.getUserId()),
                attendanceRepository.countByUser_UserId(friendUser.getUserId()),
                avatarService.getAvatarProfile(friendUser.getUserId()),
                friend.getCreatedAt(),
                friend.getAcceptedAt()
        );
    }

    private FriendRequestResponse toFriendRequestResponse(Friend friend, Long currentUserId,
                                                          List<AvatarEquippedItemResponse> requesterEquippedItems) {
        return new FriendRequestResponse(
                friend.getFriendId(),
                friend.getRequester().getUserId(),
                friend.getRequester().getNickname(),
                friend.getReceiver().getUserId(),
                friend.getReceiver().getNickname(),
                visibleStatus(friend, currentUserId),
                resolveDirection(friend, currentUserId),
                friend.getCreatedAt(),
                requesterEquippedItems
        );
    }

    private FriendProfileResponse toFriendProfileResponse(User friendUser) {
        AvatarProfileResponse avatarProfile = avatarService.getAvatarProfile(friendUser.getUserId());
        // 친구 방은 프로필 상세에서만 내려준다. 목록에 넣으면 친구 수만큼 배치를 읽게 된다.
        RoomResponse room = roomService.getRoom(friendUser.getUserId());
        return new FriendProfileResponse(
                friendUser.getUserId(),
                friendUser.getNickname(),
                friendUser.getLevel(),
                friendUser.getSchoolName(),
                friendUser.isSchoolVerified(),
                friendUser.getStatus().name(),
                hasAttendedToday(friendUser.getUserId()),
                attendanceRepository.countByUser_UserId(friendUser.getUserId()),
                lastAttendanceDate(friendUser.getUserId()),
                avatarProfile,
                room,
                friendUser.isLearningPublic() ? toSubjectStats(friendUser.getUserId()) : List.of(),
                friendUser.isLearningPublic()
        );
    }

    /**
     * 친구 프로필의 과목별 스탯(FR-FRIEND-02 시스템처리 = 프로필 조회가 과목별 스탯을 함께 준다).
     * 본인 마이페이지와 같은 progress 도메인 집계를 쓰되 대상만 친구로 바꾼다.
     * 친구 관계(ACCEPTED) 검증은 호출부인 getProfile 이 먼저 수행한다.
     */
    private List<FriendSubjectStatResponse> toSubjectStats(Long targetUserId) {
        return progressService.getSubjectProgress(targetUserId).stream()
                .map(progress -> new FriendSubjectStatResponse(
                        progress.subjectId(),
                        progress.solvedQuestionCount(),
                        progress.correctQuestionCount(),
                        progress.wrongQuestionCount(),
                        progress.achievementRate()))
                .toList();
    }

    private FriendPokeResponse toFriendPokeResponse(FriendPoke poke) {
        return new FriendPokeResponse(
                poke.getPokeId(),
                poke.getFromUser().getUserId(),
                poke.getFromUser().getNickname(),
                poke.getToUser().getUserId(),
                poke.getToUser().getNickname(),
                poke.getMessageType(),
                poke.getCreatedAt()
        );
    }

    private FriendSearchUserResponse toFriendSearchUserResponse(Long currentUserId, User user,
                                                                Optional<Friend> relation,
                                                                List<AvatarEquippedItemResponse> equippedItems) {
        return new FriendSearchUserResponse(
                user.getUserId(),
                user.getNickname(),
                user.getSchoolName(),
                user.getLevel(),
                relation.map(friend -> visibleStatus(friend, currentUserId)).orElse(null),
                relation.map(friend -> resolveDirection(friend, currentUserId)).orElse(null),
                equippedItems
        );
    }

    // 내가 보낸 요청이 거절됐으면 나에게는 계속 대기 중으로 보여 준다. 거절은 알리지 않는다(FR-FRIEND-04).
    private FriendStatus visibleStatus(Friend friend, Long currentUserId) {
        boolean rejectedMyRequest = friend.getStatus() == FriendStatus.REJECTED
                && friend.getRequester().getUserId().equals(currentUserId);
        return rejectedMyRequest ? FriendStatus.REQUESTED : friend.getStatus();
    }

    private boolean hasAttendedToday(Long userId) {
        return attendanceRepository.existsByUser_UserIdAndAttendanceDate(userId, LocalDate.now());
    }

    // 친구 프로필 "최근 출석" 표시용: 가장 최근 출석일(없으면 null)
    private LocalDate lastAttendanceDate(Long userId) {
        return attendanceRepository.findTopByUser_UserIdOrderByAttendanceDateDesc(userId)
                .map(Attendance::getAttendanceDate)
                .orElse(null);
    }

    private void validateNotSelf(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new CoreMateException(ErrorCode.FRIEND_SELF_REQUEST);
        }
    }

    private void validateRespondStatus(FriendStatus status) {
        if (status != FriendStatus.ACCEPTED && status != FriendStatus.REJECTED) {
            throw new CoreMateException(ErrorCode.INVALID_FRIEND_STATUS);
        }
    }

    private String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return REQUEST_DIRECTION_ALL;
        }
        String normalized = direction.trim().toUpperCase();
        if (REQUEST_DIRECTION_RECEIVED.equals(normalized) || REQUEST_DIRECTION_SENT.equals(normalized)) {
            return normalized;
        }
        return REQUEST_DIRECTION_ALL;
    }

    private String resolveDirection(Friend friend, Long currentUserId) {
        return friend.getReceiver().getUserId().equals(currentUserId)
                ? REQUEST_DIRECTION_RECEIVED
                : REQUEST_DIRECTION_SENT;
    }

    private Long lowId(Long userId, Long friendUserId) {
        return Math.min(userId, friendUserId);
    }

    private Long highId(Long userId, Long friendUserId) {
        return Math.max(userId, friendUserId);
    }
}
