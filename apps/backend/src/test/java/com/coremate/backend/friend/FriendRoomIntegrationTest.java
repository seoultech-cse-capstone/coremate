package com.coremate.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest;
import com.coremate.backend.avatar.dto.request.SaveRoomPlacementsRequest.Placement;
import com.coremate.backend.avatar.dto.request.SaveRoomPoseRequest;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.RoomPose;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.avatar.service.RoomService;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.entity.Friend;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 방 조회 통합 테스트(H2). 친구 프로필에 친구의 방과 마지막 포즈가 실려 오는지,
 * 그리고 경과 시간 라벨이 함께 오는지 확인한다(FR-AVATAR-04, SCR-FRIEND-02).
 */
@SpringBootTest
@Transactional
@DisplayName("친구 방 조회 (SCR-FRIEND-02)")
class FriendRoomIntegrationTest {

    @Autowired
    private FriendService friendService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private EntityManager entityManager;

    private User me;
    private User friend;

    @BeforeEach
    void seed() {
        me = userRepository.save(User.builder()
                .email("me@test.com").passwordHash("h").name("나")
                .nickname("나테스트").phoneNumber("01000001111").role(UserRole.USER).build());
        friend = userRepository.save(User.builder()
                .email("friend@test.com").passwordHash("h").name("친구")
                .nickname("친구테스트").phoneNumber("01000002222").role(UserRole.USER).build());
        userAvatarProfileRepository.save(new UserAvatarProfile(me));
        userAvatarProfileRepository.save(new UserAvatarProfile(friend));

        Friend friendship = new Friend(me, friend);
        friendship.accept(LocalDateTime.now());
        friendRepository.save(friendship);

        // 친구가 침대를 놓고 자는 포즈를 취한 상태로 만든다.
        entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES ('침대', 'FURNITURE', NULL, 'COMMON', 'bed.png', FALSE,"
                        + " ? FORMAT JSON, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter(1, "{\"footprint\":{\"w\":2,\"h\":2},\"interaction\":\"bed\"}")
                .executeUpdate();
        Long bedItemId = ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = '침대'")
                .getSingleResult()).longValue();
        UserItem bed = userItemRepository.save(new UserItem(
                friend, entityManager.find(AvatarItem.class, bedItemId), 1, "RANDOM_BOX", null));
        entityManager.flush();

        var room = roomService.savePlacements(friend.getUserId(), new SaveRoomPlacementsRequest(
                List.of(new Placement(bed.getUserItemId(), 2, 3))));
        entityManager.flush();
        roomService.savePose(friend.getUserId(), new SaveRoomPoseRequest(
                "SLEEP_BED", room.placements().get(0).placementId()));
        entityManager.flush();
    }

    @Test
    @DisplayName("친구 프로필에 친구의 방이 그대로 실려 온다")
    void friendProfileCarriesRoom() {
        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());

        assertThat(profile.room()).isNotNull();
        assertThat(profile.room().userId()).isEqualTo(friend.getUserId());
        assertThat(profile.room().placements()).hasSize(1);
        assertThat(profile.room().placements().get(0).gridX()).isEqualTo(2);
        assertThat(profile.room().placements().get(0).gridY()).isEqualTo(3);
        assertThat(profile.room().gridWidth()).isEqualTo(7);
    }

    @Test
    @DisplayName("친구가 마지막에 취한 포즈로 보인다")
    void friendProfileCarriesPose() {
        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());

        assertThat(profile.room().roomPose()).isEqualTo(RoomPose.SLEEP_BED);
        assertThat(profile.room().roomPosePlacementId()).isNotNull();
        assertThat(profile.avatarProfile().roomPose()).isEqualTo(RoomPose.SLEEP_BED);
    }

    // ⚠️ 포즈는 접속 상태가 아니다. 라벨이 빠지면 "지금 자고 있다"로 오해한다.
    @Test
    @DisplayName("경과 시간 라벨이 함께 온다")
    void friendProfileCarriesElapsedLabel() {
        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), friend.getUserId());

        assertThat(profile.room().roomPoseElapsedText()).isNotNull();
        assertThat(profile.avatarProfile().roomPoseElapsedText()).isNotNull();
        // 방금 저장했으므로 첫 구간이다.
        assertThat(profile.room().roomPoseElapsedText()).isEqualTo("방금 전부터");
    }

    @Test
    @DisplayName("포즈를 한 번도 바꾸지 않은 친구는 라벨이 없다")
    void noLabelWhenPoseNeverSet() {
        User another = userRepository.save(User.builder()
                .email("plain@test.com").passwordHash("h").name("무포즈")
                .nickname("무포즈테스트").phoneNumber("01000003333").role(UserRole.USER).build());
        userAvatarProfileRepository.save(new UserAvatarProfile(another));
        Friend friendship = new Friend(me, another);
        friendship.accept(LocalDateTime.now());
        friendRepository.save(friendship);
        entityManager.flush();

        FriendProfileResponse profile = friendService.getProfile(me.getUserId(), another.getUserId());

        assertThat(profile.room().roomPose()).isNull();
        assertThat(profile.room().roomPoseElapsedText()).isNull();
        assertThat(profile.room().placements()).isEmpty();
    }
}
