//package com.beggar.api.controller;
//
//import com.beggar.api.dto.user.UserRequest;
//import com.beggar.api.security.JwtTokenProvider;
//import com.beggar.api.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/users")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//    // ToDO : Post /users/signup            - 회원가입
//    @PostMapping("/signup")
//    public ResponseEntity<?> signup(@RequestBody UserRequest userRequest){
//        userService.userSignup(userRequest);
//        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
//    }
//    // 로그인
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody UserRequest userRequest){
//        boolean result = UserService.login(userRequest);
//        if{
//            String token = JwtTokenProvider.createToken(userRequest.getEmail());
//            System.out.println("userRequest = " + userRequest);
//            return ResponseEntity.ok("로그인 성공");
//                    .header("Authorization", "Bearer" + token)
//                    .body(true);
//        }
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 틀렸습니다");
//    }
//     TODO: GET    /users/me               — 내 프로필 조회
//     TODO: GET    /users/me/beggar-score  — 내 거지력 점수/칭호
//     TODO: DELETE /users/me               — 회원 탈퇴
//}
