package com.udangtangtang.backend.service;

import com.udangtangtang.backend.domain.User;
import com.udangtangtang.backend.domain.UserRole;
import com.udangtangtang.backend.dto.request.SocialLoginRequestDto;
import com.udangtangtang.backend.dto.request.SignupRequestDto;
import com.udangtangtang.backend.dto.request.TokenRequestDto;
import com.udangtangtang.backend.dto.request.UserRequestDto;
import com.udangtangtang.backend.dto.response.JwtTokenResponseDto;
import com.udangtangtang.backend.dto.response.LoginResponseDto;
import com.udangtangtang.backend.exception.ApiRequestException;
import com.udangtangtang.backend.repository.UserRepository;
import com.udangtangtang.backend.security.UserDetailsImpl;
import com.udangtangtang.backend.security.kakao.KakaoOAuth2;
import com.udangtangtang.backend.security.kakao.KakaoUserInfo;
import com.udangtangtang.backend.util.JwtTokenUtil;
import com.udangtangtang.backend.util.RedisUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final RedisUtil redisUtil;
    private final KakaoOAuth2 kakaoOAuth2;
    private static final String ADMIN_TOKEN = "AAABnv/xRVklrnYxKZ0aHgTBcXukeZygoC";


    @Transactional
    public User createUser(SignupRequestDto signupRequestDto) throws ApiRequestException {
        String username = signupRequestDto.getUsername();
        // 사용자이름 중복 확인
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isPresent()) {
            throw new ApiRequestException("중복된 사용자 이름이 존재합니다.");
        }

        String password = passwordEncoder.encode(signupRequestDto.getPassword());
        String email = signupRequestDto.getEmail();
        UserRole role = UserRole.USER;

        User user = new User(username, password, email, role);
        return userRepository.save(user);
    }

    public ResponseEntity<?> createAuthenticationToken(UserRequestDto userRequestDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userRequestDto.getUsername(), userRequestDto.getPassword()));

        final String accessToken = jwtTokenUtil.generateAccessToken(userRequestDto.getUsername());
        final String refreshToken = jwtTokenUtil.generateRefreshToken();

        // FIXME] 현재 사용자 식별자는 username 이 아닌 userId
        // Redis 에 Refresh Token 저장
        redisUtil.setValue("RT:" + authentication.getName(), refreshToken, JwtTokenUtil.REFRESH_TOKEN_EXP_TIME);

        final UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(userRequestDto.getUsername());

        return ResponseEntity.ok(new LoginResponseDto(userDetails.getId(), userRequestDto.getUsername(), accessToken, refreshToken));
    }

    public ResponseEntity<?> createAuthenticationTokenByKakao(SocialLoginRequestDto socialLoginRequestDto) {
        /* 카카오 정보 데이터베이스에 저장 */
        KakaoUserInfo userInfo = kakaoOAuth2.getUserInfo(socialLoginRequestDto.getToken());
        Long kakaoId = userInfo.getId();
        String nickname = userInfo.getNickname();
        String email = userInfo.getEmail();

        String username = nickname;
        String password = kakaoId + ADMIN_TOKEN;

        // 카카오 아이디 중복 확인
        User kakaoUser = userRepository.findByKakaoId(kakaoId)
                .orElse(null);

        // 카카오 정보 저장
        if (kakaoUser == null) {
            String encodedPassword = passwordEncoder.encode(password);
            UserRole role = UserRole.USER;

            kakaoUser = new User(nickname, encodedPassword, email, role, kakaoId);
            userRepository.save(kakaoUser);
        }

        /* 로그인 처리 */
        Authentication kakaoUsernamePassword = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(kakaoUsernamePassword);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        /* 토큰 처리 */
        final String accessToken = jwtTokenUtil.generateAccessToken(username);
        final String refreshToken = jwtTokenUtil.generateRefreshToken();

        // Redis 에 Refresh Token 저장
        redisUtil.setValue("RT:" + authentication.getName(), refreshToken, JwtTokenUtil.REFRESH_TOKEN_EXP_TIME);

        final UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        return ResponseEntity.ok(new LoginResponseDto(userDetails.getId(), username, accessToken, refreshToken));
    }

    public ResponseEntity<?> reissueAuthenticationToken(TokenRequestDto tokenRequestDto) {
        // 사용자로부터 받은 Refresh Token 유효성 검사
        // Refresh Token 마저 만료되면 다시 로그인
        if(jwtTokenUtil.isTokenExpired(tokenRequestDto.getRefreshToken()) || !jwtTokenUtil.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new IllegalArgumentException("잘못된 요청입니다. 다시 로그인해주세요.");
        }

        // Access Token 에 기술된 사용자 이름 가져오기
        String username = jwtTokenUtil.getUsernameFromToken(tokenRequestDto.getAccessToken());

        // Redis 에 저장된 Refresh Token 과 비교
        String refreshToken = redisUtil.getValue("RT:" + username);
        if(ObjectUtils.isEmpty(refreshToken)) {
            throw new IllegalArgumentException("잘못된 요청입니다. 다시 로그인해주세요.");
        }
        if(!refreshToken.equals(tokenRequestDto.getRefreshToken())) {
            throw new IllegalArgumentException("Refresh Token 정보가 일치하지 않습니다.");
        }

        // 새로운 Access Token 발급
        final String accessToken = jwtTokenUtil.generateAccessToken(username);

        return ResponseEntity.ok(new JwtTokenResponseDto(accessToken));
    }

//    public String kakaoLogin(String token) {
//        KakaoUserInfo userInfo = kakaoOAuth2.getUserInfo(token);
//        Long kakaoId = userInfo.getId();
//        String nickname = userInfo.getNickname();
//        String email = userInfo.getEmail();
//
//        String username = nickname;
//        String password = kakaoId + ADMIN_TOKEN;
//
//        // 카카오 아이디 중복 확인
//        User kakaoUser = userRepository.findByKakaoId(kakaoId)
//                .orElse(null);
//
//        // 카카오 정보 저장
//        if (kakaoUser == null) {
//            String encodedPassword = passwordEncoder.encode(password);
//            UserRole role = UserRole.USER;
//
//            kakaoUser = new User(nickname, encodedPassword, email, role, kakaoId);
//            userRepository.save(kakaoUser);
//        }
//
//        // 로그인 처리
//        Authentication kakaoUsernamePassword = new UsernamePasswordAuthenticationToken(username, password);
//        Authentication authentication = authenticationManager.authenticate(kakaoUsernamePassword);
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        return username;
//    }
}