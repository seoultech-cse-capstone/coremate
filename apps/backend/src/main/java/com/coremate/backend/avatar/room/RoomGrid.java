package com.coremate.backend.avatar.room;

// 방 격자 크기. 조회 응답(1-6)과 배치 검증(EX-ITEM-004)이 같은 값을 봐야 해서 한 곳에 둔다.
// 바꾸려면 DB 명세서 user_room_placements 운영기준 · EX-ITEM-004 · SCR-MYPAGE-04 를 함께 고친다.
//
// 2026-08-30 개정: 10x7 -> 7x5. 프론트가 화면에서 실제로 맞춰본 결과이며
// 「픽셀룸 에셋 템플릿 스펙(통합본)」 2절이 정본이다. 서버가 프론트보다 넓게 잡고 있으면
// 화면 밖 좌표를 통과시켜 "저장은 됐는데 안 보이는" 가구가 생긴다.
public final class RoomGrid {

    public static final int WIDTH = 7;
    public static final int HEIGHT = 5;

    private RoomGrid() {
    }

    public static boolean contains(int gridX, int gridY) {
        return gridX >= 0 && gridX < WIDTH && gridY >= 0 && gridY < HEIGHT;
    }
}
