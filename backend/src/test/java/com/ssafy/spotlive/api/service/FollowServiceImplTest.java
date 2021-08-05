package com.ssafy.spotlive.api.service;

import com.ssafy.spotlive.db.entity.Follow;
import com.ssafy.spotlive.db.entity.FollowId;
import com.ssafy.spotlive.db.repository.FollowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FollowServiceImplTest {

    @Autowired
    FollowService followService;
    @Autowired
    FollowRepository followRepository;

    @Test
    void 팔() {
        // 데이터 셋팅
        FollowId followId = new FollowId();
        followId.setArtist("sqk8657@naver.com");
        followId.setFan("kmk130519@naver.com");

        // 팔로우 실행
        followService.insertFollowByAccountEmail("sqk8657@naver.com","kmk130519@naver.com");

        // 영린 민권 쌍이 있는지 확인
        Optional<Follow> follow = followRepository.findById(followId);
        assertThat(follow.isPresent()).isEqualTo(true);
    }

    @Test
    void 언팔() {
        // 데이터 셋팅
        FollowId followId = new FollowId();
        followId.setArtist("sqk8657@naver.com");
        followId.setFan("kmk130519@naver.com");

        // 영린 민권 쌍이 있나 테스트
        Optional<Follow> follow = followRepository.findById(followId);
        assertThat(follow.isPresent()).isEqualTo(true);

        // 언팔로우 실행
        followService.deleteFollowByAccountEmail("sqk8657@naver.com","kmk130519@naver.com");

        // 이제는 없으면 성공
        Optional<Follow> follow2 = followRepository.findById(followId);
        assertThat(follow2.isPresent()).isEqualTo(false);
    }

    }
}