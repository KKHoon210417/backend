package com.udangtangtang.backend.service;

import com.udangtangtang.backend.domain.Article;
import com.udangtangtang.backend.domain.Likes;
import com.udangtangtang.backend.dto.LikeResponseDto;
import com.udangtangtang.backend.repository.ArticleRepository;
import com.udangtangtang.backend.repository.LikesRepository;
import com.udangtangtang.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class LikeService {

    private final LikesRepository likesRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    public List<LikeResponseDto> getLikes(Long userId) {
        List<Article> articleList = articleRepository.findAll();

        List<LikeResponseDto> likeResponseDtoList = new ArrayList<>();
        for (Article article : articleList) {
            Long likeCount = likesRepository.countByArticleId(article.getId());

            if (likesRepository.findByUserIdAndArticleId(userId, article.getId()).isPresent()) {
                likeResponseDtoList.add(new LikeResponseDto(article, likeCount, true));
            } else {
                likeResponseDtoList.add(new LikeResponseDto(article, likeCount, false));
            }
        }
        return likeResponseDtoList;
    }

    public List<LikeResponseDto> getLikesUser(Long userId) {
        List<Article> articleList = articleRepository.findAllByUserId(userId);

        List<LikeResponseDto> likeResponseDtoList = new ArrayList<>();
        for (Article article : articleList) {
            Long likeCount = likesRepository.countByArticleId(article.getId());

            if (likesRepository.findByUserIdAndArticleId(userId, article.getId()).isPresent()) {
                likeResponseDtoList.add(new LikeResponseDto(article, likeCount, true));
            } else {
                likeResponseDtoList.add(new LikeResponseDto(article, likeCount, false));
            }
        }
        return likeResponseDtoList;
    }


    public List<LikeResponseDto> getLikesGuest() {
        List<Article> articleList = articleRepository.findAll();

        List<LikeResponseDto> likeResponseDtoList = new ArrayList<>();
        for (Article article : articleList) {
            Long likeCount = likesRepository.countByArticleId(article.getId());
            likeResponseDtoList.add(new LikeResponseDto(article, likeCount, false));
        }
        return likeResponseDtoList;
    }


    public LikeResponseDto getLike(Long id, Long userId) {
        Article article = articleRepository.findById(id).orElseThrow(
                () -> new NullPointerException("해당되는 아이디의 게시물이 없습니다.")
        );
        Long likeCount = likesRepository.countByArticleId(id);

        if (likesRepository.findByUserIdAndArticleId(userId, id).isPresent()) {
            LikeResponseDto likeResponseDto = new LikeResponseDto(article, likeCount, true);
            return likeResponseDto;
        } else {
            LikeResponseDto likeResponseDto = new LikeResponseDto(article, likeCount, false);
            return likeResponseDto;
        }
    }

    public LikeResponseDto getLikeGuest(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(
                () -> new NullPointerException("해당되는 아이디의 게시물이 없습니다.")
        );
        Long likeCount = likesRepository.countByArticleId(id);
        LikeResponseDto likeResponseDto = new LikeResponseDto(article, likeCount, false);

        return likeResponseDto;
    }

    @Transactional
    public void increaseLikeCount(Long userId, Long articleId) {
        if(likesRepository.findByUserIdAndArticleId(userId, articleId).isPresent()) {
            throw new IllegalArgumentException("이미 좋아요를 누른 게시글입니다.");
        }
        userRepository.findById(userId).orElseThrow(
                () -> new NullPointerException("해당 유저가 존재하지 않습니다!")
        );
        articleRepository.findById(articleId).orElseThrow(
                () -> new NullPointerException("해당 글이 존재하지 않습니다!")
        );
        likesRepository.save(new Likes(userId, articleId));
    }

    @Transactional
    public void decreaseLikeCount(Long user, Long articleId) {
        Likes deleteLike = likesRepository.findByUserIdAndArticleId(user, articleId).orElseThrow(
                () -> new NullPointerException("해당 좋아요 항목이 존재하지 않습니다!")
        );
        userRepository.findById(user).orElseThrow(
                () -> new NullPointerException("해당 유저가 존재하지 않습니다!")
        );
        articleRepository.findById(articleId).orElseThrow(
                () -> new NullPointerException("해당 글이 존재하지 않습니다!")
        );
        likesRepository.delete(deleteLike);
    }
}
