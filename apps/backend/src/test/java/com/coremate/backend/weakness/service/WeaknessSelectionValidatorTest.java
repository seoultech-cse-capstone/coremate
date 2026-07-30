package com.coremate.backend.weakness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coremate.backend.content.entity.Subject;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.solve.dto.type.SolveSessionStatus;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.repository.SolveSessionRepository;
import com.coremate.backend.weakness.config.WeaknessAnalysisProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeaknessSelectionValidatorTest {

    @Mock
    private SolveSessionRepository solveSessionRepository;

    private WeaknessSelectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WeaknessSelectionValidator(solveSessionRepository, new WeaknessAnalysisProperties());
    }

    @Test
    void rejectsEmptySelection() {
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of()));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_SELECTION_EMPTY);
    }

    @Test
    void rejectsTooManySessions() {
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L, 2L, 3L, 4L, 5L, 6L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_SELECTION_TOO_MANY);
    }

    @Test
    void rejectsDuplicateSessions() {
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L, 1L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_DUPLICATE_SESSION);
    }

    @Test
    void rejectsWhenSomeSessionMissingOrNotOwned() {
        SolveSession only = session(SolveSessionStatus.COMPLETED, SolveSessionType.SUBJECT_MOCK, 1L);
        when(solveSessionRepository.findBySolveSessionIdInAndUserId(any(), anyLong()))
                .thenReturn(List.of(only));
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L, 2L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_SESSION_NOT_FOUND);
    }

    @Test
    void rejectsNotCompletedSession() {
        SolveSession inProgress = session(SolveSessionStatus.IN_PROGRESS, SolveSessionType.SUBJECT_MOCK, 1L);
        when(solveSessionRepository.findBySolveSessionIdInAndUserId(any(), anyLong()))
                .thenReturn(List.of(inProgress));
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_SESSION_NOT_COMPLETED);
    }

    @Test
    void rejectsNonMockSession() {
        SolveSession conceptSession = session(SolveSessionStatus.COMPLETED, SolveSessionType.CONCEPT_PRACTICE, 1L);
        when(solveSessionRepository.findBySolveSessionIdInAndUserId(any(), anyLong()))
                .thenReturn(List.of(conceptSession));
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_SESSION_NOT_MOCK);
    }

    @Test
    void rejectsMixedSubject() {
        SolveSession subjectA = session(SolveSessionStatus.COMPLETED, SolveSessionType.SUBJECT_MOCK, 1L);
        SolveSession subjectB = session(SolveSessionStatus.COMPLETED, SolveSessionType.CATEGORY_LV2_MOCK, 2L);
        when(solveSessionRepository.findBySolveSessionIdInAndUserId(any(), anyLong()))
                .thenReturn(List.of(subjectA, subjectB));
        CoreMateException ex = assertThrows(CoreMateException.class,
                () -> validator.validateAndLoad(1L, List.of(1L, 2L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WEAKNESS_MIXED_SUBJECT);
    }

    @Test
    void acceptsValidSameSubjectMockSelection() {
        List<SolveSession> sessions = List.of(
                session(SolveSessionStatus.COMPLETED, SolveSessionType.SUBJECT_MOCK, 1L),
                session(SolveSessionStatus.COMPLETED, SolveSessionType.CATEGORY_LV2_MOCK, 1L));
        when(solveSessionRepository.findBySolveSessionIdInAndUserId(any(), anyLong())).thenReturn(sessions);

        List<SolveSession> result = validator.validateAndLoad(1L, List.of(1L, 2L));

        assertThat(result).hasSize(2);
    }

    private SolveSession session(SolveSessionStatus status, SolveSessionType type, long subjectId) {
        Subject subject = mock(Subject.class);
        lenient().when(subject.getSubjectId()).thenReturn(subjectId);
        SolveSession session = mock(SolveSession.class);
        lenient().when(session.getStatus()).thenReturn(status);
        lenient().when(session.getSessionType()).thenReturn(type);
        lenient().when(session.getSubject()).thenReturn(subject);
        return session;
    }
}
