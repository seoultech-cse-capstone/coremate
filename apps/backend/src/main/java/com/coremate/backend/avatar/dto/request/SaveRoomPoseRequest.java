package com.coremate.backend.avatar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "아바타 포즈 저장 요청. 앱이 자동으로 보내며 사용자가 직접 입력하지 않는다")
public record SaveRoomPoseRequest(
        // 문자열로 받는다. enum 으로 받으면 잘못된 값이 Jackson 단계에서 터져 400 대신 500 이 나간다.
        @NotBlank
        @Schema(description = "STAND / SIT_DESK / SLEEP_BED. 그리는 포즈 4종과 달리 저장값은 3종이다",
                example = "SLEEP_BED") String roomPose,

        @Schema(description = "가구 상호작용 포즈일 때 그 가구의 배치 ID. STAND 면 무시된다",
                example = "12") Long placementId
) {
}
