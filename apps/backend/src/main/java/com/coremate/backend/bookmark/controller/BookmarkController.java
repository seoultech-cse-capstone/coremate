package com.coremate.backend.bookmark.controller;

import com.coremate.backend.bookmark.dto.request.CreateBookmarkRequest;
import com.coremate.backend.bookmark.dto.request.UpdateBookmarkMemoRequest;
import com.coremate.backend.bookmark.dto.response.BookmarkResponse;
import com.coremate.backend.bookmark.dto.response.BookmarkedQuestionResponse;
import com.coremate.backend.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark")
@RestController
@RequestMapping("/api/bookmarks")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class BookmarkController {

    @Operation(summary = "내 북마크 조회")
    @GetMapping
    public List<BookmarkResponse> getBookmarks() {
        return List.of(BookmarkResponse.builder()
                .bookmarkId(10L)
                .userId(42L)
                .questionId(101L)
                .memo("Review this later")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    @Operation(summary = "북마크한 문제 목록 조회")
    @GetMapping("/questions")
    public List<BookmarkedQuestionResponse> getBookmarkedQuestions() {
        return List.of(BookmarkedQuestionResponse.builder()
                .questionId(101L)
                .conceptId(55L)
                .questionFormat("MCQ_4")
                .questionType("SINGLE")
                .difficultyLevel(3)
                .bookmarkedAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    @Operation(summary = "북마크 생성")
    @PostMapping
    public BookmarkResponse create(@RequestBody CreateBookmarkRequest request) {
        return BookmarkResponse.builder()
                .bookmarkId(11L)
                .userId(42L)
                .questionId(request.getQuestionId())
                .memo(request.getMemo())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Operation(summary = "북마크 메모 수정")
    @PutMapping("/{bookmarkId}/memo")
    public BookmarkResponse updateMemo(@PathVariable Long bookmarkId, @RequestBody UpdateBookmarkMemoRequest request) {
        return BookmarkResponse.builder()
                .bookmarkId(bookmarkId)
                .userId(42L)
                .questionId(101L)
                .memo(request.getMemo())
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
