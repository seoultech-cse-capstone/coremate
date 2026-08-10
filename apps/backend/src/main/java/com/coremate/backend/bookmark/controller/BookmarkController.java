package com.coremate.backend.bookmark.controller;

import com.coremate.backend.bookmark.dto.request.CreateBookmarkRequest;
import com.coremate.backend.bookmark.dto.request.UpdateBookmarkMemoRequest;
import com.coremate.backend.bookmark.dto.response.BookmarkResponse;
import com.coremate.backend.bookmark.dto.response.BookmarkedQuestionDetailResponse;
import com.coremate.backend.bookmark.dto.response.BookmarkedQuestionResponse;
import com.coremate.backend.bookmark.service.BookmarkService;
import com.coremate.backend.global.config.OpenApiConfig;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "내 북마크 조회")
    @GetMapping
    public List<BookmarkResponse> getBookmarks(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return bookmarkService.getBookmarks(userDetails.getUserId());
    }

    @Operation(summary = "북마크한 문제 목록 조회")
    @GetMapping("/questions")
    public List<BookmarkedQuestionResponse> getBookmarkedQuestions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return bookmarkService.getBookmarkedQuestions(userDetails.getUserId());
    }

    @Operation(summary = "북마크한 문제 상세 조회",
            description = "북마크한 문제의 본문·선택지·정답·해설을 조회한다. 내가 북마크한 활성 문제만 조회할 수 있다.")
    @GetMapping("/questions/{questionId}")
    public BookmarkedQuestionDetailResponse getBookmarkedQuestionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long questionId
    ) {
        return bookmarkService.getBookmarkedQuestionDetail(userDetails.getUserId(), questionId);
    }

    @Operation(summary = "북마크 생성")
    @PostMapping
    public BookmarkResponse create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateBookmarkRequest request
    ) {
        return bookmarkService.create(userDetails.getUserId(), request);
    }

    @Operation(summary = "북마크 메모 수정")
    @PutMapping("/{bookmarkId}/memo")
    public BookmarkResponse updateMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookmarkId,
            @Valid @RequestBody UpdateBookmarkMemoRequest request
    ) {
        return bookmarkService.updateMemo(userDetails.getUserId(), bookmarkId, request);
    }

    @Operation(summary = "북마크 삭제")
    @DeleteMapping("/{bookmarkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookmarkId
    ) {
        bookmarkService.delete(userDetails.getUserId(), bookmarkId);
    }
}
