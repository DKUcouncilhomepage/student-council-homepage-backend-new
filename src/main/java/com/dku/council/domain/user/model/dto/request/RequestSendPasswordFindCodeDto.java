package com.dku.council.domain.user.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestSendPasswordFindCodeDto {

    @NotBlank
    @Schema(description = "학번", example = "32111111")
    private final String studentId;

    @NotBlank
    @Pattern(regexp = "\\d{3}-*\\d{4}-*\\d{4}")
    @Schema(description = "휴대폰 번호. 대시(-)는 있어도 되고 없어도 된다.", example = "010-1111-2222")
    private final String phoneNumber;
}
