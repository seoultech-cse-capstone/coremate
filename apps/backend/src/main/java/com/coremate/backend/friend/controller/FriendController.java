package com.coremate.backend.friend.controller;

import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendResponse;
import com.coremate.backend.friend.dto.response.FriendSearchUserResponse;
import com.coremate.backend.friend.dto.response.FriendSubjectStatResponse;
import com.coremate.backend.friend.service.FriendService;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.learninghistory.dto.response.LearningHistoryResponse;
import com.coremate.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Friend")
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 목록 조회")
    @GetMapping
    public List<FriendResponse> getFriends(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return friendService.getFriends(userDetails.getUserId());
    }

    @Operation(summary = "친구 추가용 사용자 검색")
    @GetMapping("/search")
    public List<FriendSearchUserResponse> searchUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String keyword
    ) {
        return friendService.searchUsers(userDetails.getUserId(), keyword);
    }

    @Operation(summary = "친구 요청 목록 조회")
    @GetMapping("/requests")
    public List<FriendRequestResponse> getRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "ALL") String direction
    ) {
        return friendService.getRequests(userDetails.getUserId(), direction);
    }

    @Operation(summary = "친구 요청 보내기")
    @PostMapping("/requests")
    public FriendRequestResponse sendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SendFriendRequest request
    ) {
        return friendService.sendRequest(userDetails.getUserId(), request);
    }

    @Operation(summary = "친구 요청 응답")
    @PostMapping("/requests/respond")
    public FriendResponse respond(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RespondFriendRequest request
    ) {
        return friendService.respond(userDetails.getUserId(), request);
    }

    @Operation(summary = "친구 관계 삭제")
    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendUserId
    ) {
        friendService.delete(userDetails.getUserId(), friendUserId);
    }

    @Operation(summary = "친구 프로필 조회")
    @GetMapping("/{friendUserId}/profile")
    public FriendProfileResponse getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendUserId
    ) {
        return friendService.getProfile(userDetails.getUserId(), friendUserId);
    }

    @Operation(summary = "친구 과목 통계 조회")
    @GetMapping("/{friendUserId}/subject-stats")
    public List<FriendSubjectStatResponse> getSubjectStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendUserId
    ) {
        return friendService.getSubjectStats(userDetails.getUserId(), friendUserId);
    }

    @Operation(summary = "친구 Learning History 조회",
            description = "친구 상세 프로필의 Learning History 섹션. 친구 관계(ACCEPTED)인 경우에만 조회 가능하며, "
                    + "모의고사 총 풀이 시간/개념 문제집 distinct 정답 수/PASS한 개념 수를 반환한다. 기록이 없으면 0.")
    @GetMapping("/{friendUserId}/learning-history")
    public LearningHistoryResponse getLearningHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendUserId
    ) {
        return friendService.getLearningHistory(userDetails.getUserId(), friendUserId);
    }

    @Operation(
            summary = "친구 찌르기",
            description = "친구 관계(ACCEPTED)인 사용자에게만 보낼 수 있다. "
                    + "같은 친구에게 1일 3회, 전체 1일 30회까지 보낼 수 있으며 한도는 매일 0시(KST)에 초기화된다."
    )
    @PostMapping("/pokes")
    public FriendPokeResponse poke(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PokeFriendRequest request
    ) {
        return friendService.poke(userDetails.getUserId(), request);
    }

    @Operation(summary = "받은 친구 콕 찌르기 조회")
    @GetMapping("/pokes")
    public List<FriendPokeResponse> getReceivedPokes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly
    ) {
        return friendService.getReceivedPokes(userDetails.getUserId(), unreadOnly);
    }

    @Operation(summary = "친구 콕 찌르기 읽음 처리")
    @PatchMapping("/pokes/{pokeId}/read")
    public FriendPokeResponse markPokeRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long pokeId
    ) {
        return friendService.markPokeRead(userDetails.getUserId(), pokeId);
    }
}
