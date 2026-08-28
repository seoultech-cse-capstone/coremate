package com.coremate.backend.avatar.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.dto.response.RoomPlacementResponse;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.room.RoomGrid;
import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.avatar.room.UserRoomPlacementRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 내 방·친구 방 조회. 장착 12슬롯과 배치 가구, 마지막 포즈를 한 번에 내려준다.
// 아바타 좌표는 저장하지도 반환하지도 않는다 - 포즈 위치는 가구 좌표에서 앱이 유도한다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final AvatarService avatarService;
    private final UserAvatarProfileRepository userAvatarProfileRepository;
    private final UserRoomPlacementRepository userRoomPlacementRepository;
    private final ObjectMapper objectMapper;

    // 친구 방도 같은 경로를 쓴다(1-10). 읽기 전용이라 소유자 구분이 필요 없다.
    @Transactional
    public RoomResponse getRoom(Long userId) {
        // 프로필 생성과 기본 지급 8종 보충이 여기서 함께 일어난다.
        var avatarProfile = avatarService.getAvatarProfile(userId);
        UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RESOURCE_NOT_FOUND));

        List<UserRoomPlacement> placements =
                userRoomPlacementRepository.findByUser_UserIdOrderByGridYAscGridXAsc(userId);

        return new RoomResponse(
                userId,
                profile.getBaseAvatarType(),
                RoomGrid.WIDTH,
                RoomGrid.HEIGHT,
                avatarProfile.equippedItems(),
                placements.stream().map(this::toPlacementResponse).toList(),
                profile.getRoomPose(),
                validPosePlacementId(profile, placements),
                profile.getRoomPoseUpdatedAt()
        );
    }

    /**
     * 포즈가 가리키는 배치가 아직 살아 있는지 확인한다.
     * FK 가 ON DELETE SET NULL 이 아니라 서비스가 직접 끊는 구조라, 조회 쪽에도 안전장치를 둔다.
     * 가구를 뺀 뒤라면 null 을 내려 앱이 기본 자리에 서서 시작하게 한다.
     */
    private Long validPosePlacementId(UserAvatarProfile profile, List<UserRoomPlacement> placements) {
        RoomPose pose = profile.getRoomPose();
        UserRoomPlacement posePlacement = profile.getRoomPosePlacement();
        if (pose == null || !pose.requiresPlacement() || posePlacement == null) {
            return null;
        }
        Set<Long> alive = placements.stream()
                .map(UserRoomPlacement::getPlacementId)
                .collect(Collectors.toSet());
        if (!alive.contains(posePlacement.getPlacementId())) {
            log.warn("포즈가 사라진 배치를 가리킨다. userId={}, placementId={}",
                    profile.getUser().getUserId(), posePlacement.getPlacementId());
            return null;
        }
        return posePlacement.getPlacementId();
    }

    private RoomPlacementResponse toPlacementResponse(UserRoomPlacement placement) {
        UserItem userItem = placement.getUserItem();
        AvatarItem item = userItem.getItem();
        return new RoomPlacementResponse(
                placement.getPlacementId(),
                userItem.getUserItemId(),
                item.getItemId(),
                item.getItemName(),
                item.getImageUrl(),
                placement.getGridX(),
                placement.getGridY(),
                readMetadata(item)
        );
    }

    // metadata 는 해석하지 않고 그대로 넘긴다. 문자열로 내리면 클라이언트가 두 번 파싱해야 해서
    // JSON 그대로 실어 보낸다. 컬럼이 json 타입이라 깨진 값은 애초에 저장되지 않는다.
    private JsonNode readMetadata(AvatarItem item) {
        if (item.getMetadata() == null || item.getMetadata().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(item.getMetadata());
        } catch (Exception e) {
            log.warn("아이템 metadata 를 읽지 못했다. itemId={}", item.getItemId(), e);
            return null;
        }
    }
}
