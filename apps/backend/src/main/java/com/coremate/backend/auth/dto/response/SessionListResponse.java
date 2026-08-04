package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "기기 목록 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {

    @Schema(description = "해지되지 않은 기기 목록. 마지막 접속이 최근인 순서다.")
    private List<SessionItem> sessions;
}
