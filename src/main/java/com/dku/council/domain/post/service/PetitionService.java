package com.dku.council.domain.post.service;

import com.dku.council.domain.comment.model.dto.CommentDto;
import com.dku.council.domain.comment.service.CommentService;
import com.dku.council.domain.post.exception.DuplicateCommentException;
import com.dku.council.domain.post.model.PetitionStatus;
import com.dku.council.domain.post.model.dto.response.ResponsePetitionDto;
import com.dku.council.domain.post.model.entity.posttype.Petition;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PetitionService {

    private final GenericPostService<Petition> postService;
    private final CommentService commentService;

    @Value("${app.post.petition.threshold-comment-count}")
    private final int thresholdCommentCount;


    @Transactional(readOnly = true)
    public ResponsePetitionDto findOnePetition(Long postId, Long userId, String remoteAddress) {
        return postService.findOne(postId, userId, remoteAddress, ResponsePetitionDto::new);
    }

    public void reply(Long postId, String answer) {
        Petition post = postService.findPost(postId);
        post.replyAnswer(answer);
        post.updatePetitionStatus(PetitionStatus.ANSWERED);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> listComment(Long postId, Pageable pageable) {
        return commentService.list(postId, pageable);
    }

    public Long createComment(Long postId, Long userId, String text, boolean isAdmin) {
        Petition post = postService.findPost(postId);

        if (!isAdmin && commentService.isCommentedAlready(postId, userId)) {
            throw new DuplicateCommentException();
        }

        if (post.getExtraStatus() == PetitionStatus.ACTIVE && post.getComments().size() + 1 >= thresholdCommentCount) { // todo 댓글 수 캐싱
            post.updatePetitionStatus(PetitionStatus.WAITING);
        }

        return commentService.create(postId, userId, text);
    }

    public Long deleteComment(Long id, Long userId) {
        return commentService.delete(id, userId, true);
    }
}
