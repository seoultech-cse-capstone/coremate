package com.coremate.backend.friend.controller;

import com.coremate.backend.friend.dto.request.PokeFriendRequest;
import com.coremate.backend.friend.dto.request.RespondFriendRequest;
import com.coremate.backend.friend.dto.request.SendFriendRequest;
import com.coremate.backend.friend.dto.response.FriendPokeResponse;
import com.coremate.backend.friend.dto.response.FriendProfileResponse;
import com.coremate.backend.friend.dto.response.FriendRequestResponse;
import com.coremate.backend.friend.dto.response.FriendResponse;
import com.coremate.backend.friend.dto.response.FriendSubjectStatResponse;
import com.coremate.backend.friend.dto.type.FriendStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Friend")
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Operation(summary = "친구 목록 조회")
    @GetMapping
    public List<FriendResponse> getFriends() {
        return List.of(new FriendResponse(
                123L,
                42L,
                99L,
                FriendStatus.ACCEPTED,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2)
        ));
    }

    @Operation(summary = "친구 요청 목록 조회")
    @GetMapping("/requests")
    public List<FriendRequestResponse> getRequests() {
        return List.of(new FriendRequestResponse(
                124L,
                77L,
                42L,
                FriendStatus.REQUESTED,
                LocalDateTime.now().minusHours(5)
        ));
    }

    @Operation(summary = "친구 요청 보내기")
    @PostMapping("/requests")
    public FriendRequestResponse sendRequest(@RequestBody SendFriendRequest request) {
        return new FriendRequestResponse(
                125L,
                42L,
                request.getReceiverId(),
                FriendStatus.REQUESTED,
                LocalDateTime.now()
        );
    }

    @Operation(summary = "친구 요청 응답")
    @PostMapping("/requests/respond")
    public FriendResponse respond(@RequestBody RespondFriendRequest request) {
        return new FriendResponse(
                request.getFriendId(),
                77L,
                42L,
                request.getStatus(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now()
        );
    }

    @Operation(summary = "친구 프로필 조회")
    @GetMapping("/{friendUserId}/profile")
    public FriendProfileResponse getProfile(@PathVariable Long friendUserId) {
        return new FriendProfileResponse(friendUserId, "friend", 5, "ACTIVE", "https://cdn.example.com/avatar.png");
    }

    @Operation(summary = "친구 과목 통계 조회")
    @GetMapping("/{friendUserId}/subject-stats")
    public List<FriendSubjectStatResponse> getSubjectStats(@PathVariable Long friendUserId) {
        return List.of(new FriendSubjectStatResponse(1L, 120, 90, 30, "0.75"));
    }

    @Operation(summary = "친구 찌르기")
    @PostMapping("/pokes")
    public FriendPokeResponse poke(@RequestBody PokeFriendRequest request) {
        return new FriendPokeResponse(
                1L,
                42L,
                request.getToUserId(),
                request.getMessageType(),
                false,
                LocalDateTime.now()
        );
    }
}
