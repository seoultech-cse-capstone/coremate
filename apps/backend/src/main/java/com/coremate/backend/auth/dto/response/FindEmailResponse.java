package com.coremate.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "아이디(이메일) 찾기 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindEmailResponse {

    @Schema(description = "마스킹된 가입 이메일 목록", example = "[\"lan****@gmail.com\"]")
    private List<String> emails;
}
