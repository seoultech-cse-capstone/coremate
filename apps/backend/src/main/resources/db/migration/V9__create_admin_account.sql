-- =============================================================================
-- V9__create_admin_account.sql
--
-- 관리자 웹(coremate-admin) 로그인용 운영자 계정을 만든다. 이메일(@admin.prod)이
-- 실제 수신 불가라 앱 회원가입(이메일 인증)을 못 하므로, 계정을 직접 넣는다.
--
-- password_hash 는 BCrypt($2y$10$...) 단방향 해시다(평문은 git 에 두지 않고 팀에 별도 공유).
-- 최초 로그인 후 비밀번호를 변경하는 것을 권장한다.
--
-- 이미 같은 이메일이 있으면 건너뛴다(WHERE NOT EXISTS) — 재적용 시 UNIQUE 충돌로
-- 마이그레이션이 실패하지 않게 한다.
-- =============================================================================

INSERT INTO users (
    email, password_hash, name, nickname, role,
    level, total_xp, school_verified, learning_public, status,
    created_at, updated_at
)
SELECT
    'coremateAdmin@admin.prod',
    '$2y$10$Sa0b6XLCSo7U1HCHALhINu.WPl1owelC1ujHysj3SL4egPhukACfy',
    '운영자',
    'coremate-admin',
    'ADMIN',
    1, 0, 0, 1, 'ACTIVE',
    NOW(6), NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'coremateAdmin@admin.prod'
);
