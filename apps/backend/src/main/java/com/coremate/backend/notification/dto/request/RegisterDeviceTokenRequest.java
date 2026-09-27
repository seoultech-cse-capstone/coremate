package com.coremate.backend.notification.dto.request;

import com.coremate.backend.notification.entity.type.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "푸시 기기 토큰 등록 요청")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    @Schema(description = "FCM 등록 토큰", example = "ePZgicvRTgGJVbPLTkt36p:APA91b...")
    @NotBlank
    @Size(max = 255)
    private String token;

    @Schema(example = "ANDROID")
    @NotNull
    private DevicePlatform platform;

    @Schema(description = "기기 식별자. 인증용 기기 세션의 device_id 와 같은 값", example = "a1b2c3d4")
    @Size(max = 100)
    private String deviceId;
}
