package com.coremate.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AvatarSlot;
import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItem;
import com.coremate.backend.avatar.equip.UserAvatarEquippedItemRepository;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.profile.UserAvatarProfileRepository;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserRole;
import com.coremate.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 검색·받은 요청 목록에 상대의 장착 아이템이 실려 오는지 확인한다(H2).
 * 목록 아이콘용이라 프로필을 만들거나 기본 아이템을 채우지 않고 지금 장착만 읽어야 한다.
 */
@SpringBootTest
@Transactional
@DisplayName("친구 검색·받은 요청의 장착 아이템")
class FriendListAvatarIntegrationTest {

    @Autowired
    private FriendService friendService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserItemRepository userItemRepository;
    @Autowired
    private UserAvatarEquippedItemRepository userAvatarEquippedItemRepository;
    @Autowired
    private UserAvatarProfileRepository userAvatarProfileRepository;
    @Autowired
    private EntityManager entityManager;

    private User me;
    private User wizard;

    @BeforeEach
    void seed() {
        me = createUser("list-me@test.com", "목록나");
        wizard = createUser("list-wizard@test.com", "모자쓴친구");

        // 마법사 모자를 쓴 사용자를 만든다.
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
                wizard, entityManager.find(AvatarItem.class, hatItemId), 1, "RANDOM_BOX", null));
        userAvatarEquippedItemRepository.save(new UserAvatarEquippedItem(wizard, AvatarSlot.HAT, hat));
        entityManager.flush();
    }

    @Test
    @DisplayName("친구 검색 결과에 상대가 장착한 아이템이 실려 온다")
    void searchResultCarriesEquippedItems() {
        FriendSearchUserResponse found = friendService.searchUsers(me.getUserId(), "모자쓴").get(0);

        assertThat(found.equippedItems())
                .extracting(AvatarEquippedItemResponse::slot, AvatarEquippedItemResponse::imageUrl)
                .containsExactly(tuple(AvatarSlot.HAT, "/assets/avatar/hat_wizard.png"));
    }

    @Test
    @DisplayName("받은 요청에 보낸 사람이 장착한 아이템이 실려 온다")
    void receivedRequestCarriesRequesterEquippedItems() {
        friendService.sendRequest(wizard.getUserId(),
                SendFriendRequest.builder().receiverId(me.getUserId()).build());

        FriendRequestResponse received = friendService.getRequests(me.getUserId(), "RECEIVED").get(0);

        assertThat(received.requesterEquippedItems())
                .extracting(AvatarEquippedItemResponse::slot)
                .containsExactly(AvatarSlot.HAT);
    }

    // 남이 검색했다고 그 사람의 아바타 프로필이 생기거나 기본 아이템이 지급되면 안 된다.
    @Test
    @DisplayName("장착이 없는 사용자는 빈 배열이고, 검색만으로 프로필이 생기지 않는다")
    void emptyWithoutEquipAndNoProfileCreated() {
        User plain = createUser("list-plain@test.com", "맨머리친구");

        FriendSearchUserResponse found = friendService.searchUsers(me.getUserId(), "맨머리").get(0);

        assertThat(found.equippedItems()).isEmpty();
        assertThat(userAvatarProfileRepository.findByUser_UserId(plain.getUserId())).isEmpty();
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
