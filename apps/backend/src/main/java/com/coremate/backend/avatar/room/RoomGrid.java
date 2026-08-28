package com.coremate.backend.avatar.room;

// 방 격자 크기. 조회 응답(1-6)과 배치 검증(EX-ITEM-004)이 같은 값을 봐야 해서 한 곳에 둔다.
// 바꾸려면 DB 명세서 user_room_placements 운영기준 · EX-ITEM-004 · SCR-MYPAGE-04 를 함께 고친다.
public final class RoomGrid {

    public static final int WIDTH = 10;
    public static final int HEIGHT = 7;

    private RoomGrid() {
    }

    public static boolean contains(int gridX, int gridY) {
        return gridX >= 0 && gridX < WIDTH && gridY >= 0 && gridY < HEIGHT;
    }
}
