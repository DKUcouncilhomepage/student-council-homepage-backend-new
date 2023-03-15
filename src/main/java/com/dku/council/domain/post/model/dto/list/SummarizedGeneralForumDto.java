package com.dku.council.domain.post.model.dto.list;

import com.dku.council.domain.post.model.entity.posttype.GeneralForum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class SummarizedGeneralForumDto extends SummarizedGenericPostDto {

    // TODO 댓글수 캐싱
    @Schema(description = "댓글수", example = "5")
    private final int comments;

    public SummarizedGeneralForumDto(SummarizedGenericPostDto dto, GeneralForum generalForum) {
        super(dto);
        this.comments = generalForum.getComments().size();
    }
}
