package com.coremate.backend.avatar.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.ItemMetadataReader;
import com.coremate.backend.avatar.catalog.type.AvatarItemType;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.SaveRoomPoseRequest;
import com.coremate.backend.avatar.dto.response.RoomPlacementResponse;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.room.RoomGrid;
import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.avatar.room.UserRoomPlacementRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final ItemMetadataReader itemMetadataReader;
    private final EntityManager entityManager;

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
     * 마지막에 취한 포즈를 저장한다. 좌표는 저장하지 않는다 - 위치는 가구에서 유도한다.
     * 앱이 자동으로 보내는 값이라 실패해도 사용자에게 보여줄 안내는 없다(EX-ITEM-008).
     */
    @Transactional
    public RoomResponse savePose(Long userId, SaveRoomPoseRequest request) {
        RoomPose pose = RoomPose.from(request.roomPose());
        UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RESOURCE_NOT_FOUND));

        UserRoomPlacement placement = pose.requiresPlacement()
                ? resolvePosePlacement(userId, pose, request.placementId())
                : null;

        profile.updateRoomPose(pose, placement, LocalDateTime.now());
        return getRoom(userId);
    }

    /**
     * 가구 상호작용 포즈의 배치를 검증한다. ① 요청자 소유이고 ② 그 포즈에 맞는 가구여야 한다.
     * 침대에서 타이핑하거나 남의 가구를 가리키는 포즈가 저장되면 앱이 엉뚱한 자리에 아바타를 그린다.
     */
    private UserRoomPlacement resolvePosePlacement(Long userId, RoomPose pose, Long placementId) {
        if (placementId == null) {
            throw new CoreMateException(ErrorCode.INVALID_ROOM_POSE);
        }
        UserRoomPlacement placement = userRoomPlacementRepository.findById(placementId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new CoreMateException(ErrorCode.INVALID_ROOM_POSE));

        String interaction = itemMetadataReader.interaction(placement.getUserItem().getItem());
        if (!pose.getRequiredInteraction().equals(interaction)) {
            throw new CoreMateException(ErrorCode.INVALID_ROOM_POSE);
        }
        return placement;
    }

    /**
     * 사용자의 배치를 통째로 교체한다. 검증 넷 중 하나라도 걸리면 아무것도 바꾸지 않는다.
     * 그래서 검증을 전부 끝낸 뒤에야 삭제·삽입을 시작한다.
     */
    @Transactional
    public RoomResponse savePlacements(Long userId, SaveRoomPlacementsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));

        List<PlannedPlacement> planned = validate(userId, request.placements());

        // ⚠️ 포즈를 먼저 끊는다. FK 가 ON DELETE SET NULL 이 아니라서, 포즈가 가리키는 배치를
        //    그냥 지우려 하면 MySQL 이 ERROR 1451 로 거부해 저장 전체가 실패한다.
        userAvatarProfileRepository.findByUser_UserId(userId)
                .ifPresent(UserAvatarProfile::clearRoomPose);
        // 포즈 UPDATE 와 배치 DELETE 는 서로 다른 테이블이라 자동 flush 대상이 아니다.
        // 명시적으로 밀어내지 않으면 DELETE 가 먼저 나가 위 FK 위반이 그대로 난다.
        entityManager.flush();

        userRoomPlacementRepository.deleteByUser_UserId(userId);
        entityManager.flush();

        for (PlannedPlacement placement : planned) {
            userRoomPlacementRepository.save(new UserRoomPlacement(
                    user, placement.userItem(), placement.gridX(), placement.gridY()));
        }
        return getRoom(userId);
    }

    private List<PlannedPlacement> validate(Long userId, List<SaveRoomPlacementsRequest.Placement> requested) {
        Map<Long, UserItem> ownedById = new HashMap<>();
        for (UserItem userItem : userItemRepository.findByUser_UserIdOrderByAcquiredAtDesc(userId)) {
            ownedById.put(userItem.getUserItemId(), userItem);
        }

        List<PlannedPlacement> planned = new ArrayList<>();
        Map<Long, Integer> usedByUserItem = new HashMap<>();

        for (SaveRoomPlacementsRequest.Placement placement : requested) {
            // ① 소유·FURNITURE 여부 (EX-ITEM-003)
            UserItem userItem = ownedById.get(placement.userItemId());
            if (userItem == null || !userItem.isOwned()) {
                throw new CoreMateException(ErrorCode.FURNITURE_NOT_OWNED);
            }
            AvatarItem item = userItem.getItem();
            if (!AvatarItemType.from(item.getItemType()).isFurniture()) {
                throw new CoreMateException(ErrorCode.FURNITURE_NOT_OWNED);
            }

            // ② 격자 범위 (EX-ITEM-004). 원점뿐 아니라 footprint 를 펼친 끝칸까지 방 안이어야 한다.
            ItemMetadataReader.Footprint footprint = itemMetadataReader.footprint(item);
            if (!RoomGrid.contains(placement.gridX(), placement.gridY())
                    || !RoomGrid.contains(placement.gridX() + footprint.width() - 1,
                            placement.gridY() + footprint.height() - 1)) {
                throw new CoreMateException(ErrorCode.ROOM_PLACEMENT_OUT_OF_BOUNDS);
            }

            // ④ 보유 수량 초과 (EX-ITEM-006)
            int used = usedByUserItem.merge(placement.userItemId(), 1, Integer::sum);
            if (used > userItem.getQuantity()) {
                throw new CoreMateException(ErrorCode.ROOM_PLACEMENT_EXCEEDS_QUANTITY);
            }

            planned.add(new PlannedPlacement(userItem, placement.gridX(), placement.gridY(), footprint));
        }

        // ③ footprint 겹침 (EX-ITEM-005).
        //    UNIQUE(user_id, grid_x, grid_y) 는 원점만 막으므로 사각형 겹침은 여기서 직접 본다.
        for (int i = 0; i < planned.size(); i++) {
            for (int j = i + 1; j < planned.size(); j++) {
                if (planned.get(i).overlaps(planned.get(j))) {
                    throw new CoreMateException(ErrorCode.ROOM_PLACEMENT_OVERLAP);
                }
            }
        }
        return planned;
    }

    private record PlannedPlacement(UserItem userItem, int gridX, int gridY,
                                    ItemMetadataReader.Footprint footprint) {

        boolean overlaps(PlannedPlacement other) {
            return gridX < other.gridX + other.footprint.width()
                    && other.gridX < gridX + footprint.width()
                    && gridY < other.gridY + other.footprint.height()
                    && other.gridY < gridY + footprint.height();
        }
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
                itemMetadataReader.read(item)
        );
    }

}
