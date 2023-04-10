package com.dku.council.domain.post.service;

import com.dku.council.domain.post.exception.PostCooltimeException;
import com.dku.council.domain.post.exception.PostNotFoundException;
import com.dku.council.domain.post.model.dto.request.RequestCreateGeneralForumDto;
import com.dku.council.domain.post.model.entity.posttype.GeneralForum;
import com.dku.council.domain.post.repository.PostTimeMemoryRepository;
import com.dku.council.util.ClockUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.dku.council.domain.post.service.GeneralForumService.GENERAL_FORUM_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneralForumServiceTest {

    private final Clock clock = ClockUtil.create();
    private final Duration writeCooltime = Duration.ofDays(1);

    @Mock
    private PostTimeMemoryRepository postTimeMemoryRepository;

    @Mock
    private GenericPostService<GeneralForum> postService;

    private GeneralForumService generalForumService;

    @BeforeEach
    public void setup() {
        generalForumService = new GeneralForumService(postService, postTimeMemoryRepository, clock,
                writeCooltime);
    }

    @Test
    @DisplayName("글 작성 - 쿨타임 이전에 작성한 적 없는 경우")
    public void create() {
        // given
        RequestCreateGeneralForumDto dto = new RequestCreateGeneralForumDto("title", "body",
                List.of(), List.of());
        Instant now = Instant.now(clock);

        when(postTimeMemoryRepository.isAlreadyContains(GENERAL_FORUM_KEY, 1L, now))
                .thenReturn(false);
        when(postService.create(1L, dto)).thenReturn(5L);

        // when
        Long result = generalForumService.create(1L, dto);

        // then
        assertThat(result).isEqualTo(5L);
        verify(postTimeMemoryRepository).put(GENERAL_FORUM_KEY, 1L, writeCooltime, now);
    }

    @Test
    @DisplayName("글 작성 - 이미 작성한 경우")
    public void createAlreadyPostJustNow() {
        // given
        RequestCreateGeneralForumDto dto = new RequestCreateGeneralForumDto("title", "body",
                List.of(), List.of());
        Instant now = Instant.now(clock);

        when(postTimeMemoryRepository.isAlreadyContains(GENERAL_FORUM_KEY, 1L, now))
                .thenReturn(true);

        // when
        Assertions.assertThrows(PostCooltimeException.class,
                () -> generalForumService.create(1L, dto));
    }

    @Test
    @DisplayName("글 작성 - 생성 도중 exception 발생시 쿨타임 적용 안하기")
    public void noCooltimeCreatingException() {
        // given
        RequestCreateGeneralForumDto dto = new RequestCreateGeneralForumDto("title", "body",
                List.of(), List.of());
        Instant now = Instant.now(clock);

        when(postTimeMemoryRepository.isAlreadyContains(GENERAL_FORUM_KEY, 1L, now))
                .thenReturn(false);
        when(postService.create(1L, dto))
                .thenThrow(new PostNotFoundException());

        // when
        try {
            generalForumService.create(1L, dto);
        } catch (PostNotFoundException ignored) {
        }
        verify(postTimeMemoryRepository, never()).put(GENERAL_FORUM_KEY, 1L, writeCooltime, now);
    }
}