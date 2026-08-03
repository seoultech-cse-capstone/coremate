package com.coremate.backend.bookmark.service;

import com.coremate.backend.bookmark.dto.request.CreateBookmarkRequest;
import com.coremate.backend.bookmark.dto.request.UpdateBookmarkMemoRequest;
import com.coremate.backend.bookmark.dto.response.BookmarkResponse;
import com.coremate.backend.bookmark.dto.response.BookmarkedQuestionDetailResponse;
import com.coremate.backend.bookmark.dto.response.BookmarkedQuestionResponse;
import com.coremate.backend.bookmark.entity.Bookmark;
import com.coremate.backend.bookmark.repository.BookmarkRepository;
import com.coremate.backend.content.entity.Question;
import com.coremate.backend.content.repository.QuestionRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.problemset.dto.response.QuestionChoiceResponse;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    // 로그인 사용자의 북마크 기본 목록을 조회한다.
    public List<BookmarkResponse> getBookmarks(Long userId) {
        findUser(userId);
        return bookmarkRepository.findActiveBookmarksByUserId(userId)
                .stream()
                .map(this::toBookmarkResponse)
                .toList();
    }

    // 마이페이지/즐겨찾기 화면에서 문제 정보와 함께 보여줄 목록.
    public List<BookmarkedQuestionResponse> getBookmarkedQuestions(Long userId) {
        findUser(userId);
        return bookmarkRepository.findActiveBookmarkedQuestionsByUserId(userId)
                .stream()
                .map(this::toBookmarkedQuestionResponse)
                .toList();
    }

    // 즐겨찾기 탭에서 북마크한 문제의 상세(본문·선택지·정답·해설)를 조회한다. 내 활성 북마크만 접근할 수 있다.
    public BookmarkedQuestionDetailResponse getBookmarkedQuestionDetail(Long userId, Long questionId) {
        Bookmark bookmark = bookmarkRepository.findByUser_UserIdAndQuestion_QuestionId(userId, questionId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.BOOKMARK_NOT_FOUND));

        Question question = bookmark.getQuestion();
        if (!question.isActive()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return toBookmarkedQuestionDetailResponse(bookmark, question);
    }

    // 이미 등록된 문제는 중복 저장하지 않고 기존 북마크를 그대로 반환한다.
    @Transactional
    public BookmarkResponse create(Long userId, CreateBookmarkRequest request) {
        User user = findUser(userId);
        Question question = findActiveQuestion(request.getQuestionId());

        return bookmarkRepository.findByUser_UserIdAndQuestion_QuestionId(userId, request.getQuestionId())
                .map(this::toBookmarkResponse)
                .orElseGet(() -> {
                    Bookmark bookmark = new Bookmark(
                            user,
                            question,
                            request.getSourceSolveSessionId(),
                            request.getMemo()
                    );
                    return toBookmarkResponse(bookmarkRepository.save(bookmark));
                });
    }

    // 북마크 소유자만 메모를 수정할 수 있다.
    @Transactional
    public BookmarkResponse updateMemo(Long userId, Long bookmarkId, UpdateBookmarkMemoRequest request) {
        Bookmark bookmark = findMyBookmark(userId, bookmarkId);
        bookmark.updateMemo(request.getMemo());
        return toBookmarkResponse(bookmark);
    }

    // 즐겨찾기 해제는 현재 DB 명세에 맞춰 실제 삭제로 처리한다.
    @Transactional
    public void delete(Long userId, Long bookmarkId) {
        Bookmark bookmark = findMyBookmark(userId, bookmarkId);
        bookmarkRepository.delete(bookmark);
    }

    private Bookmark findMyBookmark(Long userId, Long bookmarkId) {
        return bookmarkRepository.findByBookmarkIdAndUser_UserId(bookmarkId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.BOOKMARK_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private Question findActiveQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.QUESTION_NOT_FOUND));
        if (!question.isActive()) {
            throw new CoreMateException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return question;
    }

    private BookmarkResponse toBookmarkResponse(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .userId(bookmark.getUser().getUserId())
                .questionId(bookmark.getQuestion().getQuestionId())
                .sourceSolveSessionId(bookmark.getSourceSolveSessionId())
                .memo(bookmark.getMemo())
                .createdAt(bookmark.getCreatedAt())
                .updatedAt(bookmark.getUpdatedAt())
                .build();
    }

    private BookmarkedQuestionResponse toBookmarkedQuestionResponse(Bookmark bookmark) {
        Question question = bookmark.getQuestion();
        return BookmarkedQuestionResponse.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .questionId(question.getQuestionId())
                .conceptId(question.getConcept().getConceptId())
                .questionFormat(question.getQuestionFormat())
                .questionType(question.getQuestionType())
                .difficultyLevel((int) question.getDifficultyLevel())
                .memo(bookmark.getMemo())
                .bookmarkedAt(bookmark.getCreatedAt())
                .build();
    }

    private BookmarkedQuestionDetailResponse toBookmarkedQuestionDetailResponse(Bookmark bookmark, Question question) {
        return BookmarkedQuestionDetailResponse.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .questionId(question.getQuestionId())
                .conceptId(question.getConcept().getConceptId())
                .conceptName(question.getConcept().getConceptName())
                .questionFormat(question.getQuestionFormat())
                .questionType(question.getQuestionType())
                .difficultyLevel((int) question.getDifficultyLevel())
                .questionText(question.getQuestionText())
                .choices(parseChoices(question.getChoicesJson()))
                .correctAnswer(question.getCorrectAnswer())
                .explanationText(question.getExplanationText())
                .expectedSolveSec(question.getExpectedSolveSec())
                .memo(bookmark.getMemo())
                .build();
    }

    private List<QuestionChoiceResponse> parseChoices(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CoreMateException(ErrorCode.INVALID_CONTENT_DATA);
        }
    }
}
