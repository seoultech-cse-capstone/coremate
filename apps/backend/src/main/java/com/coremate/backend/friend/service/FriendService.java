package com.coremate.backend.friend.service;

import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.avatar.dto.response.AvatarProfileResponse;
import com.coremate.backend.avatar.service.AvatarService;
import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.dto.response.FriendSubjectStatResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.entity.Friend;
import com.coremate.backend.friend.entity.FriendPoke;
import com.coremate.backend.friend.repository.FriendPokeRepository;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.friend.repository.FriendUserSearchRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.learninghistory.dto.response.LearningHistoryResponse;
import com.coremate.backend.learninghistory.service.LearningHistoryService;
import com.coremate.backend.notification.service.NotificationMessages;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final LearningHistoryService learningHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    public List<FriendSearchUserResponse> searchUsers(Long userId, String keyword) {
        findActiveUser(userId);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // 친구 추가 화면에서 너무 많은 사용자가 노출되지 않도록 상위 20명만 반환한다.
        return friendUserSearchRepository.searchActiveUsers(
                        userId,
                        keyword.trim(),
                        UserStatus.ACTIVE,
                        PageRequest.of(0, 20)
                )
                .stream()
                .map(user -> toFriendSearchUserResponse(userId, user))
                .toList();
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
            case REQUEST_DIRECTION_RECEIVED -> friendRepository
                    .findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendStatus.REQUESTED);
            case REQUEST_DIRECTION_SENT -> friendRepository
                    .findByRequester_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendStatus.REQUESTED);
            default -> friendRepository.findRequestedByUserId(userId, FriendStatus.REQUESTED);
        };
        return requests.stream()
                .map(friend -> toFriendRequestResponse(friend, userId))
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

        return toFriendRequestResponse(friend, userId);
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

    @Transactional
    public void delete(Long userId, Long friendUserId) {
        validateNotSelf(userId, friendUserId);
        Friend friend = findAnyRelation(userId, friendUserId);
        if (!friend.involves(userId)) {
            throw new CoreMateException(ErrorCode.FRIEND_NOT_FOUND);
        }
        friendRepository.delete(friend);
    }

    @Transactional
    public FriendProfileResponse getProfile(Long userId, Long friendUserId) {
        validateAcceptedFriend(userId, friendUserId);
        User friendUser = findActiveUser(friendUserId);
        return toFriendProfileResponse(friendUser);
    }

    public List<FriendSubjectStatResponse> getSubjectStats(Long userId, Long friendUserId) {
        validateAcceptedFriend(userId, friendUserId);
        // 과목별 통계 테이블이 연결되면 이 지점에서 공개 요약만 매핑한다.
        return List.of();
    }

    // 친구 상세 프로필 Learning History 섹션. 친구 관계(ACCEPTED)인 경우에만 조회 가능하다. (FR-FRIEND-05)
    public LearningHistoryResponse getLearningHistory(Long userId, Long friendUserId) {
        validateAcceptedFriend(userId, friendUserId);
        findActiveUser(friendUserId);
        return learningHistoryService.getStats(friendUserId);
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

    private Friend reopenRequestIfPossible(Friend friend, User requester, User receiver) {
        if (friend.getStatus() == FriendStatus.REJECTED) {
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

    private FriendRequestResponse toFriendRequestResponse(Friend friend, Long currentUserId) {
        return new FriendRequestResponse(
                friend.getFriendId(),
                friend.getRequester().getUserId(),
                friend.getRequester().getNickname(),
                friend.getReceiver().getUserId(),
                friend.getReceiver().getNickname(),
                friend.getStatus(),
                resolveDirection(friend, currentUserId),
                friend.getCreatedAt()
        );
    }

    private FriendProfileResponse toFriendProfileResponse(User friendUser) {
        AvatarProfileResponse avatarProfile = avatarService.getAvatarProfile(friendUser.getUserId());
        return new FriendProfileResponse(
                friendUser.getUserId(),
                friendUser.getNickname(),
                friendUser.getLevel(),
                friendUser.getSchoolName(),
                friendUser.isSchoolVerified(),
                friendUser.getStatus().name(),
                hasAttendedToday(friendUser.getUserId()),
                attendanceRepository.countByUser_UserId(friendUser.getUserId()),
                avatarProfile,
                List.of()
        );
    }

    private FriendPokeResponse toFriendPokeResponse(FriendPoke poke) {
        return new FriendPokeResponse(
                poke.getPokeId(),
                poke.getFromUser().getUserId(),
                poke.getFromUser().getNickname(),
                poke.getToUser().getUserId(),
                poke.getToUser().getNickname(),
                poke.getMessageType(),
                poke.isRead(),
                poke.getCreatedAt(),
                poke.getReadAt()
        );
    }

    private FriendSearchUserResponse toFriendSearchUserResponse(Long currentUserId, User user) {
        FriendStatus relationshipStatus = friendRepository
                .findByUserLowIdAndUserHighId(lowId(currentUserId, user.getUserId()), highId(currentUserId, user.getUserId()))
                .map(Friend::getStatus)
                .orElse(null);

        return new FriendSearchUserResponse(
                user.getUserId(),
                user.getNickname(),
                user.getSchoolName(),
                user.getLevel(),
                relationshipStatus
        );
    }

    private boolean hasAttendedToday(Long userId) {
        return attendanceRepository.existsByUser_UserIdAndAttendanceDate(userId, LocalDate.now());
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
