package com.udangtangtang.backend.service;

import com.udangtangtang.backend.domain.*;
import com.udangtangtang.backend.repository.ArticleRepository;
import com.udangtangtang.backend.repository.HashtagRepository;
import com.udangtangtang.backend.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final HashtagRepository hashtagRepository;
    private final ImageRepository imageRepository;

    private final FileProcessService fileProcessService;

    public List<Article> getArticles() {
        return articleRepository.findAll();
    }

    public Article getArticle(Long id) { return articleRepository.findById(id).orElseThrow(
            () -> new NullPointerException("해당되는 아이디의 게시물이 없습니다.")
        );
    }

    @Transactional
    public void createArticle(User user, String text, String location, List<String> hashtagNameList, List<MultipartFile> imageFileList) {
        // TODO: 주소값 확인
        Article article = articleRepository.save(new Article(text, location, user));

        for(String tag : hashtagNameList) {
            hashtagRepository.save(new Hashtag(tag, article, user.getId()));
        }

        for(MultipartFile multipartFile : imageFileList) {
            String url = fileProcessService.uploadImage(multipartFile, FileFolder.ARTICLE_IMAGES);
            imageRepository.save(new Image(url, article));
        }
    }
}
