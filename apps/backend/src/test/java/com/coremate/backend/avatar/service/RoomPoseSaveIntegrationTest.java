package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest.Placement;
import com.coremate.backend.avatar.dto.request.SaveRoomPoseRequest;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포즈 저장 통합 테스트(H2). 저장값 3종 검증, 포즈-가구 짝 검증(EX-ITEM-008),
 * 그리고 "실제로 바뀔 때만 시각 갱신"이 서비스 경로에서도 지켜지는지 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("아바타 포즈 저장 (FR-AVATAR-06)")
class RoomPoseSaveIntegrationTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private Long deskPlacementId;
    private Long bedPlacementId;
    private Long shelfPlacementId;   // interaction 이 없는 가구

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder()
                .email("pose@test.com").passwordHash("h").name("포즈")
                .nickname("포즈테스트").phoneNumber("01077778888").role(UserRole.USER).build());
        userId = user.getUserId();
        userAvatarProfileRepository.save(new UserAvatarProfile(user));

        Long desk = own(item("책상", "{\"footprint\":{\"w\":1,\"h\":1},\"interaction\":\"desk\"}"));
        Long bed = own(item("침대", "{\"footprint\":{\"w\":1,\"h\":1},\"interaction\":\"bed\"}"));
        Long shelf = own(item("선반", "{\"footprint\":{\"w\":1,\"h\":1}}"));
        entityManager.flush();

        RoomResponse room = roomService.savePlacements(userId, new SaveRoomPlacementsRequest(List.of(
                new Placement(desk, 0, 0), new Placement(bed, 2, 0), new Placement(shelf, 4, 0))));
        entityManager.flush();
        deskPlacementId = room.placements().get(0).placementId();
        bedPlacementId = room.placements().get(1).placementId();
        shelfPlacementId = room.placements().get(2).placementId();
    }

    private Long item(String name, String metadata) {
        entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES (?, 'FURNITURE', NULL, 'COMMON', 'x.png', FALSE, ? FORMAT JSON,"
                        + " TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, name).setParameter(2, metadata).executeUpdate();
        return ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = ?")
                .setParameter(1, name).getSingleResult()).longValue();
    }

    private Long own(Long itemId) {
        UserItem userItem = userItemRepository.save(new UserItem(
                user, entityManager.find(AvatarItem.class, itemId), 1, "RANDOM_BOX", null));
        return userItem.getUserItemId();
    }

    private RoomResponse savePose(String pose, Long placementId) {
        return roomService.savePose(userId, new SaveRoomPoseRequest(pose, placementId));
    }

    private LocalDateTime poseUpdatedAt() {
        return userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow().getRoomPoseUpdatedAt();
    }

    @Nested
    @DisplayName("정상 저장")
    class Saves {

        @Test
        @DisplayName("책상 포즈는 desk 가구를 가리킨 채 저장된다")
        void savesSitDesk() {
            RoomResponse room = savePose("SIT_DESK", deskPlacementId);

            assertThat(room.roomPose()).isEqualTo(RoomPose.SIT_DESK);
            assertThat(room.roomPosePlacementId()).isEqualTo(deskPlacementId);
            assertThat(room.roomPoseUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("STAND 는 가구를 비운다")
        void standClearsPlacement() {
            savePose("SLEEP_BED", bedPlacementId);
            entityManager.flush();

            RoomResponse room = savePose("STAND", bedPlacementId);

            assertThat(room.roomPose()).isEqualTo(RoomPose.STAND);
            assertThat(room.roomPosePlacementId()).isNull();
        }

        @Test
        @DisplayName("소문자로 보내도 받는다")
        void normalizesCase() {
            assertThat(savePose(" sleep_bed ", bedPlacementId).roomPose())
                    .isEqualTo(RoomPose.SLEEP_BED);
        }
    }

    @Nested
    @DisplayName("시각은 포즈가 실제로 바뀔 때만 갱신한다")
    class Timestamp {

        // 이게 깨지면 친구 방의 "3일 전부터 자는 중"이 앱을 켤 때마다 초기화된다.
        @Test
        @DisplayName("같은 포즈를 다시 보내면 시각이 그대로다")
        void samePoseKeepsTimestamp() {
            savePose("SLEEP_BED", bedPlacementId);
            entityManager.flush();
            LocalDateTime first = poseUpdatedAt();

            savePose("SLEEP_BED", bedPlacementId);
            entityManager.flush();

            assertThat(poseUpdatedAt()).isEqualTo(first);
        }

        @Test
        @DisplayName("다른 포즈로 바꾸면 시각이 갱신된다")
        void changedPoseUpdatesTimestamp() {
            savePose("SLEEP_BED", bedPlacementId);
            entityManager.flush();
            LocalDateTime first = poseUpdatedAt();

            savePose("SIT_DESK", deskPlacementId);
            entityManager.flush();

            assertThat(poseUpdatedAt()).isAfterOrEqualTo(first);
            assertThat(userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow()
                    .getRoomPose()).isEqualTo(RoomPose.SIT_DESK);
        }
    }

    @Nested
    @DisplayName("막아야 하는 요청 (EX-ITEM-008)")
    class Rejected {

        @ParameterizedTest(name = "{0} 은 저장값이 아니다")
        @ValueSource(strings = {"STAND_FRONT", "STAND_BACK", "SLEEP", "WALK", "  "})
        @DisplayName("저장하는 포즈는 3종뿐이다")
        void rejectsUndefinedPose(String pose) {
            assertThatThrownBy(() -> savePose(pose, null))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ROOM_POSE);
        }

        @Test
        @DisplayName("가구 포즈인데 배치를 안 보내면 거부")
        void rejectsMissingPlacement() {
            assertThatThrownBy(() -> savePose("SIT_DESK", null))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ROOM_POSE);
        }

        // 침대에서 타이핑하는 포즈가 저장되면 앱이 엉뚱한 자리에 아바타를 그린다.
        @Test
        @DisplayName("포즈와 가구가 안 맞으면 거부 - 침대에 SIT_DESK")
        void rejectsMismatchedFurniture() {
            assertThatThrownBy(() -> savePose("SIT_DESK", bedPlacementId))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ROOM_POSE);
        }

        @Test
        @DisplayName("interaction 이 없는 가구는 포즈 대상이 아니다")
        void rejectsFurnitureWithoutInteraction() {
            assertThatThrownBy(() -> savePose("SIT_DESK", shelfPlacementId))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ROOM_POSE);
        }

        @Test
        @DisplayName("남의 배치는 가리킬 수 없다")
        void rejectsOtherUsersPlacement() {
            User other = userRepository.save(User.builder()
                    .email("other@test.com").passwordHash("h").name("남")
                    .nickname("남의방").phoneNumber("01099990000").role(UserRole.USER).build());
            userAvatarProfileRepository.save(new UserAvatarProfile(other));
            entityManager.flush();

            assertThatThrownBy(() -> roomService.savePose(
                    other.getUserId(), new SaveRoomPoseRequest("SIT_DESK", deskPlacementId)))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ROOM_POSE);
        }

        @Test
        @DisplayName("거부되면 직전 포즈가 그대로 남는다")
        void keepsPreviousPoseOnRejection() {
            savePose("SLEEP_BED", bedPlacementId);
            entityManager.flush();

            assertThatThrownBy(() -> savePose("SIT_DESK", bedPlacementId))
                    .isInstanceOf(CoreMateException.class);

            assertThat(userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow()
                    .getRoomPose()).isEqualTo(RoomPose.SLEEP_BED);
        }
    }
}
