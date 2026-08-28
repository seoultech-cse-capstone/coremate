package com.coremate.backend.avatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest.Placement;
import com.coremate.backend.avatar.dto.response.RoomResponse;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.avatar.room.UserRoomPlacementRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 배치 저장 통합 테스트(H2). 검증 4가지(EX-ITEM-003~006)와 통째 교체,
 * 그리고 포즈를 먼저 끊지 않으면 FK 로 막히는 순서 문제를 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("방 가구 배치 저장 (FR-AVATAR-05)")
class RoomPlacementSaveIntegrationTest {

    @Autowired
    private RoomService roomService;
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
    private Long deskUserItemId;   // 2x1, 수량 1
    private Long chairUserItemId;  // 1x1, 수량 2
    private Long hatUserItemId;    // 가구가 아님

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder()
                .email("place@test.com").passwordHash("h").name("배치")
                .nickname("배치테스트").phoneNumber("01055556666").role(UserRole.USER).build());
        userId = user.getUserId();
        userAvatarProfileRepository.save(new UserAvatarProfile(user));

        deskUserItemId = own(item("책상", "FURNITURE", "{\"footprint\":{\"w\":2,\"h\":1},\"interaction\":\"desk\"}"), 1);
        chairUserItemId = own(item("의자", "FURNITURE", "{\"footprint\":{\"w\":1,\"h\":1}}"), 2);
        hatUserItemId = own(item("모자", "HAT", null), 1);
        entityManager.flush();
    }

    private Long item(String name, String type, String metadata) {
        String metaExpr = metadata == null ? "NULL" : "? FORMAT JSON";
        var q = entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES (?, ?, NULL, 'COMMON', 'x.png', FALSE, " + metaExpr + ", TRUE,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, name).setParameter(2, type);
        if (metadata != null) {
            q.setParameter(3, metadata);
        }
        q.executeUpdate();
        return ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = ?")
                .setParameter(1, name).getSingleResult()).longValue();
    }

    private Long own(Long itemId, int quantity) {
        UserItem userItem = userItemRepository.save(new UserItem(
                user, entityManager.find(AvatarItem.class, itemId), quantity, "RANDOM_BOX", null));
        return userItem.getUserItemId();
    }

    private RoomResponse save(Placement... placements) {
        return roomService.savePlacements(userId, new SaveRoomPlacementsRequest(List.of(placements)));
    }

    @Nested
    @DisplayName("정상 저장")
    class Saves {

        @Test
        @DisplayName("배치를 통째로 교체한다")
        void replacesWholeRoom() {
            save(new Placement(deskUserItemId, 0, 0), new Placement(chairUserItemId, 5, 5));
            entityManager.flush();

            RoomResponse room = save(new Placement(chairUserItemId, 2, 2));
            entityManager.flush();

            assertThat(room.placements()).hasSize(1);
            assertThat(room.placements().get(0).gridX()).isEqualTo(2);
            assertThat(userRoomPlacementRepository.findByUser_UserIdOrderByGridYAscGridXAsc(userId))
                    .hasSize(1);
        }

        @Test
        @DisplayName("빈 배열이면 방을 비운다")
        void emptyClearsRoom() {
            save(new Placement(deskUserItemId, 0, 0));
            entityManager.flush();

            RoomResponse room = roomService.savePlacements(
                    userId, new SaveRoomPlacementsRequest(List.of()));

            assertThat(room.placements()).isEmpty();
        }

        @Test
        @DisplayName("같은 가구를 보유 수량만큼 놓을 수 있다")
        void placesUpToQuantity() {
            RoomResponse room = save(new Placement(chairUserItemId, 0, 0),
                    new Placement(chairUserItemId, 1, 0));

            assertThat(room.placements()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("검증 4가지")
    class Validations {

        @Test
        @DisplayName("① 가구가 아니면 거부 - EX-ITEM-003")
        void rejectsNonFurniture() {
            assertThatThrownBy(() -> save(new Placement(hatUserItemId, 0, 0)))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FURNITURE_NOT_OWNED);
        }

        @Test
        @DisplayName("① 남의 것이거나 없는 보유 아이템이면 거부 - EX-ITEM-003")
        void rejectsUnknownUserItem() {
            assertThatThrownBy(() -> save(new Placement(999999L, 0, 0)))
                    .isInstanceOf(CoreMateException.class)
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FURNITURE_NOT_OWNED);
        }

        @Test
        @DisplayName("② 격자 밖이면 거부 - EX-ITEM-004")
        void rejectsOutOfGrid() {
            assertThatThrownBy(() -> save(new Placement(chairUserItemId, 10, 0)))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROOM_PLACEMENT_OUT_OF_BOUNDS);
        }

        // 원점은 방 안이지만 2칸짜리를 마지막 칸에 놓으면 끝칸이 밖으로 나간다.
        @Test
        @DisplayName("② footprint 를 펼쳤을 때 밖으로 나가면 거부 - EX-ITEM-004")
        void rejectsFootprintSpillingOut() {
            assertThatThrownBy(() -> save(new Placement(deskUserItemId, 9, 0)))
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROOM_PLACEMENT_OUT_OF_BOUNDS);
        }

        // ⚠️ UNIQUE(user_id, grid_x, grid_y) 는 원점만 막는다. 원점이 달라도 면이 겹칠 수 있다.
        @Test
        @DisplayName("③ 원점이 달라도 면이 겹치면 거부 - EX-ITEM-005")
        void rejectsOverlappingFootprints() {
            assertThatThrownBy(() -> save(
                    new Placement(deskUserItemId, 0, 0),   // (0,0)~(1,0) 두 칸
                    new Placement(chairUserItemId, 1, 0))) // 책상의 오른쪽 칸과 겹친다
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROOM_PLACEMENT_OVERLAP);
        }

        @Test
        @DisplayName("③ 딱 붙지만 안 겹치면 통과한다")
        void allowsAdjacentWithoutOverlap() {
            RoomResponse room = save(new Placement(deskUserItemId, 0, 0),
                    new Placement(chairUserItemId, 2, 0));

            assertThat(room.placements()).hasSize(2);
        }

        @Test
        @DisplayName("④ 보유 수량보다 많이 놓으면 거부 - EX-ITEM-006")
        void rejectsExceedingQuantity() {
            assertThatThrownBy(() -> save(
                    new Placement(chairUserItemId, 0, 0),
                    new Placement(chairUserItemId, 1, 0),
                    new Placement(chairUserItemId, 2, 0)))  // 보유 2개인데 3개
                    .extracting(e -> ((CoreMateException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROOM_PLACEMENT_EXCEEDS_QUANTITY);
        }
    }

    @Nested
    @DisplayName("실패하면 기존 배치를 그대로 둔다")
    class RollsBack {

        @Test
        @DisplayName("검증 실패 시 이전 배치가 남아 있다")
        void keepsPreviousPlacementsOnFailure() {
            save(new Placement(deskUserItemId, 0, 0));
            entityManager.flush();

            assertThatThrownBy(() -> save(new Placement(chairUserItemId, 0, 0),
                    new Placement(chairUserItemId, 0, 0)))
                    .isInstanceOf(CoreMateException.class);

            assertThat(userRoomPlacementRepository.findByUser_UserIdOrderByGridYAscGridXAsc(userId))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("포즈가 가리키는 가구를 빼는 경우")
    class PoseInterlock {

        // ⚠️ 포즈를 먼저 끊지 않으면 MySQL 이 ERROR 1451 로 DELETE 를 거부해 저장 전체가 실패한다.
        @Test
        @DisplayName("포즈가 가리키던 가구를 빼도 저장이 성공하고 포즈가 풀린다")
        void clearsPoseBeforeDeletingPlacements() {
            RoomResponse before = save(new Placement(deskUserItemId, 0, 0));
            entityManager.flush();
            Long deskPlacementId = before.placements().get(0).placementId();

            UserRoomPlacement desk = userRoomPlacementRepository.findById(deskPlacementId).orElseThrow();
            UserAvatarProfile profile = userAvatarProfileRepository.findByUser_UserId(userId).orElseThrow();
            profile.updateRoomPose(RoomPose.SIT_DESK, desk, LocalDateTime.of(2026, 8, 25, 10, 0));
            entityManager.flush();

            // 책상을 빼고 의자만 놓는다.
            RoomResponse after = save(new Placement(chairUserItemId, 4, 4));
            entityManager.flush();

            assertThat(after.placements()).hasSize(1);
            assertThat(after.roomPose()).isNull();
            assertThat(after.roomPosePlacementId()).isNull();
        }
    }
}
