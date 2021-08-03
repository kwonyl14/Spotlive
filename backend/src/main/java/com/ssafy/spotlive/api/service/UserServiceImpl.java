package com.ssafy.spotlive.api.service;

import com.ssafy.spotlive.api.response.user.KakaoUserRes;
import com.ssafy.spotlive.api.response.user.UserRes;
import com.ssafy.spotlive.db.entity.User;
import com.ssafy.spotlive.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * @FileName : UserServiceImpl
 * @작성자 : 김민권
 * @Class 설명 : User 관련 비즈니스 로직을 처리하는 Service
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoRestApiKey;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoRestSecretKey;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUrl;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUrl;

    private final String KAKAO_URL = "https://kauth.kakao.com";

    @Autowired
    private UserRepository userRepository;

    @Override
    public String getKakaoLoginUrl() {
        /**
         * @Method Name : getKakaoLoginUrl
         * @작성자 : 김민권
         * @Method 설명 : 카카오 로그인을 위한 요청 URL을 반환하는 Method, 해당 URL로 GET 요청을 전송 시 카카오톡 로그인 페이지로 이동된다.
         */
        return new StringBuilder()
                .append(KAKAO_URL).append("/oauth/authorize?client_id=").append(kakaoRestApiKey)
                .append("&redirect_uri=").append(kakaoRedirectUrl)
                .append("&response_type=code")
                .toString();
    }

    @Override
    public HashMap<String, String> getKakaoTokens(String code) {
        /**
         * @Method Name : getKakaoTokens
         * @작성자 : 김민권
         * @Method 설명 : 발급된 code를 통해 Access Token과 Refresh Token을 반환한다.
         */
        HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> httpBody = new LinkedMultiValueMap<>();
            httpBody.add("grant_type", "authorization_code");
            httpBody.add("client_id", kakaoRestApiKey);
            httpBody.add("redirect_uri", kakaoRedirectUrl);
            httpBody.add("code", code);
            httpBody.add("client_secret", kakaoRestSecretKey);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenReq = new HttpEntity<>(httpBody, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HashMap> tokenResEntity = restTemplate.exchange(KAKAO_URL + "/oauth/token", HttpMethod.POST, kakaoTokenReq, HashMap.class);

        return tokenResEntity.getBody();
    }

    @Override
    public KakaoUserRes getKakaoUserInfo(String tokenType, String accessToken) {
        /**
         * @Method Name : getKakaoUserInfo
         * @작성자 : 김민권
         * @Method 설명 : 발급된 token을 통해 kakao user 정보를 반환한다.
         */
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        httpHeaders.add("Authorization", tokenType + " " + accessToken);

        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoReq = new HttpEntity<>(httpHeaders);
        ResponseEntity<KakaoUserRes> userInfo = restTemplate.exchange(kakaoUserInfoUrl, HttpMethod.GET, kakaoUserInfoReq, KakaoUserRes.class);

        return userInfo.getBody();
    }

    @Override
    public UserRes findUserByAccountEmail(String accountEmail) {
        /**
         * @Method Name : findUserByAccountEmail
         * @작성자 : 김민권
         * @Method 설명 : 발급된 token을 통해 kakao user 정보를 반환한다.
         */

        User user = userRepository.findUserByAccountEmail(accountEmail);
        return user == null ? null : UserRes.of(userRepository.findUserByAccountEmail(accountEmail));
    }

    @Override
    public UserRes refreshTokensForExistUser(String accountEmail, String accessToken, String refreshToken) {
        /**
         * @Method Name : refreshTokensForExistUser
         * @작성자 : 김민권
         * @Method 설명 : 존재하는 회원에 대해서 각 토큰 값을 업데이트 한 후 반환한다.
         */
        User existUser = userRepository.findUserByAccountEmail(accountEmail);
        existUser.setAccessToken(accessToken);
        existUser.setRefreshToken(refreshToken);
        userRepository.save(existUser);

        return UserRes.of(existUser);
    }

    @Override
    public UserRes insertUser(User newUser) {
        /**
         * @Method Name : insertUser
         * @작성자 : 김민권
         * @Method 설명 : 존재하지않는 회원에 대해서 각 토큰 값을 포함한 user를 삽입하고 반환한다.
         */
        userRepository.save(newUser);
        return UserRes.of(newUser);
    }

    @Override
    public String accessTokenUpdate(String accountEmail) {
        User userForUpdate = userRepository.findUserByAccountEmail(accountEmail);
        if(userForUpdate == null) return null;

        String refreshToken = userForUpdate.getRefreshToken();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> httpBody = new LinkedMultiValueMap<>();
        httpBody.add("grant_type", "refresh_token");
        httpBody.add("client_id", kakaoRestApiKey);
        httpBody.add("refresh_token", refreshToken);
        httpBody.add("client_secret", kakaoRestSecretKey);

        HttpEntity<MultiValueMap<String, String>> kakaoUpdateTokenReq = new HttpEntity<>(httpBody, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HashMap> tokenResEntity = restTemplate.exchange(KAKAO_URL + "/oauth/token", HttpMethod.POST, kakaoUpdateTokenReq, HashMap.class);

        String newToken = (String) tokenResEntity.getBody().get("access_token");
        userForUpdate.setAccessToken(newToken);
        userRepository.save(userForUpdate);

        return newToken;
    }
}