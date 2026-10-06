package com.coremate.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.UserAvatarProfile;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.friend.dto.response.FriendResponse;
import com.coremate.backend.friend.entity.Friend;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 목록 조회(getFriends)가 아바타·출석을 배치로 실어 오는지, 그리고 목록을 봤다고 상대의
 * 아바타 프로필이 생기지 않는지 확인한다(#322, H2).
 */
@SpringBootTest
@Transactional
@DisplayName("친구 목록의 아바타·출석 배치 조회")
class FriendListIntegrationTest {

    @Autowired
    private FriendService friendService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("친구 목록에 상대의 장착 아바타와 출석 정보가 실려 온다")
    void getFriendsCarriesAvatarAndAttendance() {
        User me = createUser("fl-me@test.com", "목록나");
        User friend = createUser("fl-friend@test.com", "모자친구");
        userAvatarProfileRepository.save(new UserAvatarProfile(friend));
        equipHat(friend);
        // 친구는 오늘 출석했다.
        attendanceRepository.save(new Attendance(friend, LocalDate.now(), null));
        acceptFriendship(friend, me);
        entityManager.flush();
        entityManager.clear();

        List<FriendResponse> friends = friendService.getFriends(me.getUserId());

        assertThat(friends).hasSize(1);
        FriendResponse response = friends.get(0);
        assertThat(response.friendUserId()).isEqualTo(friend.getUserId());
        assertThat(response.attendedToday()).isTrue();
        assertThat(response.attendanceCount()).isEqualTo(1L);
        assertThat(response.avatarProfile().equippedItems())
                .extracting(AvatarEquippedItemResponse::slot)
                .contains(AvatarSlot.HAT);
    }

    // 남을 목록에서 봤다고 그 사람의 아바타 프로필이 생기거나 기본 아이템이 지급되면 안 된다.
    @Test
    @DisplayName("프로필이 없는 친구는 기본 아바타로 내려오고, 조회만으로 프로필이 생기지 않는다")
    void getFriendsDoesNotCreateProfileForFriendWithoutOne() {
        User me = createUser("fl-me2@test.com", "목록나2");
        User plain = createUser("fl-plain@test.com", "맨머리친구");
        acceptFriendship(plain, me);
        entityManager.flush();
        entityManager.clear();

        List<FriendResponse> friends = friendService.getFriends(me.getUserId());

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).avatarProfile().baseAvatarType())
                .isEqualTo(UserAvatarProfile.DEFAULT_BASE_AVATAR_TYPE);
        assertThat(friends.get(0).attendanceCount()).isEqualTo(0L);
        assertThat(userAvatarProfileRepository.findByUser_UserId(plain.getUserId())).isEmpty();
    }

    // respond() 의 아바타 조회 부수효과를 피하려고 수락된 관계를 직접 저장한다.
    private void acceptFriendship(User requester, User receiver) {
        Friend friend = new Friend(requester, receiver);
        friend.accept(LocalDateTime.now());
        friendRepository.save(friend);
    }

    private void equipHat(User user) {
        entityManager.createNativeQuery(
                "INSERT INTO avatar_items (item_name, item_type, slot, rarity, image_url,"
                        + " is_default, metadata, is_active, created_at, updated_at)"
                        + " VALUES ('마법사 모자', 'HAT', 'HAT', 'RARE', '/assets/avatar/hat_wizard.png', FALSE,"
                        + " NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();
        Long hatItemId = ((Number) entityManager.createNativeQuery(
                "SELECT item_id FROM avatar_items WHERE item_name = '마법사 모자'")
                .getSingleResult()).longValue();
        UserItem hat = userItemRepository.save(new UserItem(
                user, entityManager.find(AvatarItem.class, hatItemId), 1, "RANDOM_BOX", null));
        userAvatarEquippedItemRepository.save(new UserAvatarEquippedItem(user, AvatarSlot.HAT, hat));
    }

    private User createUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("{noop}password")
                .nickname(nickname)
                .role(UserRole.USER)
                .build());
    }
}
