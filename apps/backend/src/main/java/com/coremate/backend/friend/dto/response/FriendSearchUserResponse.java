package com.coremate.backend.friend.dto.response;

import com.coremate.backend.avatar.dto.response.AvatarEquippedItemResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "친구 검색 사용자 응답")
public record FriendSearchUserResponse(
        @Schema(example = "99") Long userId,
        @Schema(example = "corefriend") String nickname,
        @Schema(example = "서울과학기술대학교") String schoolName,
        @Schema(example = "5") Integer level,
        @Schema(example = "REQUESTED",
                description = "검색한 사람 기준 관계 상태. 관계가 없으면 null. "
                        + "내가 보낸 요청이 거절됐어도 REQUESTED 로 보인다(거절은 알리지 않는다, FR-FRIEND-04)")
        FriendStatus relationshipStatus,
        @Schema(example = "SENT",
                description = "요청을 누가 보냈는지. SENT = 내가 보냄, RECEIVED = 상대가 보냄. 관계가 없으면 null")
        String direction,
        @Schema(description = "이 사용자가 지금 장착한 아이템(렌더 순서). 목록 아이콘용이라 장착이 없으면 빈 배열")
        List<AvatarEquippedItemResponse> equippedItems
) {
}
