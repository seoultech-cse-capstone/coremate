package com.coremate.backend.shop.controller;

import com.coremate.backend.global.security.CustomUserDetails;
import com.coremate.backend.shop.dto.request.OpenRandomBoxRequest;
import com.coremate.backend.shop.dto.response.RandomBoxItemResponse;
import com.coremate.backend.shop.dto.response.RandomBoxOpenResponse;
import com.coremate.backend.shop.dto.response.RandomBoxResponse;
import com.coremate.backend.shop.service.RandomBoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RandomBox")
@RestController
@RequestMapping("/api/random-box")
@RequiredArgsConstructor
public class RandomBoxController {

    private final RandomBoxService randomBoxService;

    @Operation(summary = "랜덤박스 목록 조회")
    @GetMapping
    public List<RandomBoxResponse> getRandomBoxes() {
        return randomBoxService.getRandomBoxes();
    }

    @Operation(summary = "랜덤박스 구성 조회")
    @GetMapping("/items")
    public List<RandomBoxItemResponse> getRandomBoxItems(
            @RequestParam(required = false) Long randomBoxId
    ) {
        return randomBoxService.getRandomBoxItems(randomBoxId);
    }

    @Operation(summary = "랜덤박스 열기")
    @PostMapping("/open")
    public RandomBoxOpenResponse open(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OpenRandomBoxRequest request
    ) {
        return randomBoxService.open(userDetails.getUserId(), request);
    }
}
