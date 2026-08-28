package com.coremate.backend.avatar.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// avatar_items.metadata 를 읽는 단일 창구.
// 백엔드가 해석하는 키는 footprint(배치 검증) 하나뿐이고 나머지는 앱에 그대로 넘긴다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemMetadataReader {

    private final ObjectMapper objectMapper;

    public JsonNode read(AvatarItem item) {
        if (item.getMetadata() == null || item.getMetadata().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(item.getMetadata());
        } catch (Exception e) {
            log.warn("아이템 metadata 를 읽지 못했다. itemId={}", item.getItemId(), e);
            return null;
        }
    }

    /**
     * 가구가 격자에서 차지하는 칸 수. FURNITURE 는 metadata.footprint 가 반드시 있어야 하지만,
     * 시드가 빠뜨렸다고 사용자 배치를 막지는 않고 1x1 로 보고 경고만 남긴다.
     */
    public Footprint footprint(AvatarItem item) {
        JsonNode metadata = read(item);
        JsonNode footprint = metadata == null ? null : metadata.get("footprint");
        if (footprint == null) {
            log.warn("FURNITURE 인데 metadata.footprint 가 없다. 1x1 로 처리한다. itemId={}", item.getItemId());
            return Footprint.SINGLE;
        }
        int width = footprint.path("w").asInt(1);
        int height = footprint.path("h").asInt(1);
        if (width < 1 || height < 1) {
            log.warn("metadata.footprint 값이 이상하다. 1x1 로 처리한다. itemId={}, w={}, h={}",
                    item.getItemId(), width, height);
            return Footprint.SINGLE;
        }
        return new Footprint(width, height);
    }

    public record Footprint(int width, int height) {
        public static final Footprint SINGLE = new Footprint(1, 1);
    }
}
