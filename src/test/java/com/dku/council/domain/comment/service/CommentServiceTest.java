package com.dku.council.domain.comment.service;

import com.dku.council.domain.comment.CommentRepository;
import com.dku.council.domain.comment.CommentStatus;
import com.dku.council.domain.comment.model.entity.Comment;
import com.dku.council.domain.post.model.entity.Post;
import com.dku.council.domain.post.repository.PostRepository;
import com.dku.council.domain.user.model.entity.User;
import com.dku.council.domain.user.repository.UserRepository;
import com.dku.council.global.error.exception.NotGrantedException;
import com.dku.council.mock.CommentMock;
import com.dku.council.mock.NewsMock;
import com.dku.council.mock.UserMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService service;


    @Test
    @DisplayName("댓글 추가")
    void create() {
        // given
        Post post = NewsMock.createDummy();
        User user = UserMock.createDummyMajor();
        Comment comment = CommentMock.createWithId(post, user);
        when(commentRepository.save(any())).thenReturn(comment);
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        when(userRepository.findById(1L)).thenReturn(Optional.ofNullable(user));

        // when
        Long created = service.create(1L, 1L, "");

        // then
        assertThat(created).isEqualTo(comment.getId());
    }

    @Test
    @DisplayName("댓글 수정 - 내가 쓴거")
    void edit() {
        // given
        User user = UserMock.createDummyMajor(10L);
        Comment comment = CommentMock.createWithId(user);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        // when
        Long edited = service.edit(10L, 10L, "new text");

        // then
        assertThat(edited).isEqualTo(10L);
        assertThat(comment.getText()).isEqualTo("new text");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 내가 쓴게 아닌경우")
    void failedEditByNotMine() {
        // given
        User user = UserMock.createDummyMajor(11L);
        Comment comment = CommentMock.createWithId(user);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        // when & then
        assertThrows(NotGrantedException.class,
                () -> service.edit(10L, 10L, "new text"));
    }

    @Test
    @DisplayName("댓글 삭제 - 내가 쓴거")
    void deleteMine() {
        // given
        User user = UserMock.createDummyMajor(10L);
        Comment comment = CommentMock.createWithId(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        // when
        Long deleted = service.delete(comment.getId(), user.getId(), false);

        // then
        assertThat(deleted).isEqualTo(comment.getId());
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    @Test
    @DisplayName("댓글 삭제 - 어드민")
    void deleteByAdmin() {
        // given
        User user = UserMock.createDummyMajor(11L);
        Comment comment = CommentMock.createWithId(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        // when
        Long deleted = service.delete(comment.getId(), 10L, true);

        // then
        assertThat(deleted).isEqualTo(comment.getId());
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED_BY_ADMIN);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없는 경우")
    void failedDeleteByNotGranted() {
        // given
        User user = UserMock.createDummyMajor(11L);
        Comment comment = CommentMock.createWithId(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        // when & then
        assertThrows(NotGrantedException.class,
                () -> service.delete(comment.getId(), 10L, false));
    }
}