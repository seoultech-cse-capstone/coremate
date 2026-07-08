package com.coremate.backend.shop.controller;

import com.coremate.backend.shop.dto.request.OpenRandomBoxRequest;
import com.coremate.backend.shop.dto.response.RandomBoxItemResponse;
import com.coremate.backend.shop.dto.response.RandomBoxOpenResponse;
import com.coremate.backend.shop.dto.response.RandomBoxResponse;
import com.coremate.backend.global.config.OpenApiConfig;
import com.coremate.backend.shop.dto.type.RandomBoxGrade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RandomBox")
@RestController
@RequestMapping("/api/random-box")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class RandomBoxController {

    @Operation(summary = "랜덤박스 목록 조회")
    @GetMapping
    public List<RandomBoxResponse> getRandomBoxes() {
        return List.of(new RandomBoxResponse(3L, "Starter Box", RandomBoxGrade.COMMON, 1, true));
    }

    @Operation(summary = "랜덤박스 구성 조회")
    @GetMapping("/items")
    public List<RandomBoxItemResponse> getRandomBoxItems() {
        return List.of(new RandomBoxItemResponse(1L, "Blue Hat", "ITEM", 50, "0.250000"));
    }

    @Operation(summary = "랜덤박스 열기")
    @PostMapping("/open")
    public RandomBoxOpenResponse open(@RequestBody OpenRandomBoxRequest request) {
        return new RandomBoxOpenResponse(
                200L,
                42L,
                request.getRandomBoxId(),
                "ITEM",
                1L,
                null,
                LocalDateTime.now()
        );
    }
}
