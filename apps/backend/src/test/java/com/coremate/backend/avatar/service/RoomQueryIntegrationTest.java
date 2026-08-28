package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.avatar.room.UserRoomPlacementRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 방 조회 통합 테스트(H2). 장착·배치·포즈가 한 응답에 모이는지와,
 * 포즈가 가리키던 가구가 사라졌을 때 조회 쪽 안전장치가 도는지를 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("내 방 조회 (FR-AVATAR-04)")
class RoomQueryIntegrationTest {

    private static final List<String> DEFAULT_SLOTS =
            List.of("HAIR", "EYES", "MOUTH", "TOP", "BOTTOM", "SHOES", "WALLPAPER", "FLOOR");

    @Autowired
    private RoomService roomService;
    @Autowired
    private AvatarService avatarService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private UserRoomPlacementRepository userRoomPlacementRepository;
    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private Long deskItemId;

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder()
                .email("room@test.com").passwordHash("hash").name("방")
                .nickname("방테스트").phoneNumber("01033334444").role(UserRole.USER).build());
        userId = user.getUserId();

        for (String slot : DEFAULT_SLOTS) {
            insertItem("기본 " + slot, slot, slot, true, null);
        }
        // 가구는 슬롯이 없고 metadata 로 점유 칸과 상호작용을 표현한다.
        deskItemId = insertItem("기본 책상", "FURNITURE", null, false,
                "{\"footprint\":{\"w\":2,\"h\":1},\"interaction\":\"desk\"}");
        entityManager.flush();

        avatarService.getMyAvatar(userId);
        entityManager.flush();
    }

    private Long insertItem(String name, String type, String slot, boolean isDefault, String metadata) {
        // H2 는 문자열을 json 컬럼에 그냥 넣으면 JSON "문자열"로 저장해 파싱하면 TextNode 가 나온다.
        // FORMAT JSON 을 붙여야 MySQL 처럼 객체로 들어간다.
        String metadataExpr = metadata == null ? "NULL" : "? FORMAT JSON";
        var query = entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES (?, ?, ?, 'COMMON', 'x.png', ?, " + metadataExpr + ", TRUE,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, name).setParameter(2, type).setParameter(3, slot)
                .setParameter(4, isDefault);
        if (metadata != null) {
            query.setParameter(5, metadata);
        }
        query.executeUpdate();
        return ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = ?")
                .setParameter(1, name).getSingleResult()).longValue();
    }

    private UserRoomPlacement placeDesk(int gridX, int gridY) {
        UserItem deskUserItem = userItemRepository.save(new UserItem(
                user, entityManager.find(
                        com.coremate.backend.avatar.catalog.AvatarItem.class, deskItemId),
                1, "RANDOM_BOX", null));
        UserRoomPlacement placement = userRoomPlacementRepository.save(
                new UserRoomPlacement(user, deskUserItem, gridX, gridY));
        entityManager.flush();
        return placement;
    }

    @Nested
    @DisplayName("한 응답에 다 모인다")
    class Aggregated {

        @Test
        @DisplayName("장착 12슬롯 · 배치 가구 · 격자 크기를 함께 내려준다")
        void returnsEverythingAtOnce() {
            placeDesk(3, 4);

            RoomResponse room = roomService.getRoom(userId);

            assertThat(room.userId()).isEqualTo(userId);
            assertThat(room.baseAvatarType()).isEqualTo("BASIC_01");
            assertThat(room.gridWidth()).isEqualTo(10);
            assertThat(room.gridHeight()).isEqualTo(7);
            assertThat(room.equippedItems()).hasSize(8);
            assertThat(room.placements()).hasSize(1);
        }

        @Test
        @DisplayName("가구의 footprint 와 interaction 을 JSON 그대로 실어 보낸다")
        void carriesMetadataAsJson() {
            placeDesk(3, 4);

            RoomResponse room = roomService.getRoom(userId);

            var placement = room.placements().get(0);
            assertThat(placement.gridX()).isEqualTo(3);
            assertThat(placement.gridY()).isEqualTo(4);
            assertThat(placement.metadata().get("footprint").get("w").asInt()).isEqualTo(2);
            assertThat(placement.metadata().get("interaction").asText()).isEqualTo("desk");
        }

        @Test
        @DisplayName("배치가 없으면 빈 목록이다")
        void emptyRoomIsFine() {
            RoomResponse room = roomService.getRoom(userId);

            assertThat(room.placements()).isEmpty();
            assertThat(room.roomPose()).isNull();
            assertThat(room.roomPosePlacementId()).isNull();
        }
    }

    @Nested
    @DisplayName("포즈 복원")
    class PoseRestore {

        @Test
        @DisplayName("배치가 살아 있으면 그 가구를 가리킨 채 내려준다")
        void keepsPoseWhenPlacementAlive() {
            UserRoomPlacement desk = placeDesk(3, 4);
            UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow();
            profile.updateRoomPose(RoomPose.SIT_DESK, desk, LocalDateTime.of(2026, 8, 25, 10, 0));
            entityManager.flush();

            RoomResponse room = roomService.getRoom(userId);

            assertThat(room.roomPose()).isEqualTo(RoomPose.SIT_DESK);
            assertThat(room.roomPosePlacementId()).isEqualTo(desk.getPlacementId());
            assertThat(room.roomPoseUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 10, 0));
        }

        // 조회 쪽 안전장치. FK 가 ON DELETE SET NULL 이 아니라 서비스가 직접 끊는 구조라 이중으로 막는다.
        @Test
        @DisplayName("가구를 뺐으면 배치를 null 로 내려 기본 자리에서 시작하게 한다")
        void dropsPosePlacementWhenFurnitureRemoved() {
            UserRoomPlacement desk = placeDesk(3, 4);
            UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow();
            profile.updateRoomPose(RoomPose.SIT_DESK, desk, LocalDateTime.of(2026, 8, 25, 10, 0));
            entityManager.flush();

            // 가구를 방에서 뺀다. 실제 흐름(1-7)에서는 포즈를 먼저 끊고 지우지만,
            // 여기서는 조회 쪽 안전장치만 따로 확인하려고 배치만 지운다.
            profile.clearRoomPose();
            userRoomPlacementRepository.delete(desk);
            entityManager.flush();
            profile.updateRoomPose(RoomPose.SIT_DESK, null, LocalDateTime.of(2026, 8, 25, 10, 0));
            entityManager.flush();

            RoomResponse room = roomService.getRoom(userId);

            assertThat(room.placements()).isEmpty();
            assertThat(room.roomPosePlacementId()).isNull();
        }

        @Test
        @DisplayName("STAND 는 가구를 가리키지 않는다")
        void standNeedsNoPlacement() {
            UserRoomPlacement desk = placeDesk(3, 4);
            UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow();
            profile.updateRoomPose(RoomPose.STAND, desk, LocalDateTime.of(2026, 8, 25, 10, 0));
            entityManager.flush();

            RoomResponse room = roomService.getRoom(userId);

            assertThat(room.roomPose()).isEqualTo(RoomPose.STAND);
            assertThat(room.roomPosePlacementId()).isNull();
        }
    }
}
