package zerobase.dividend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerobase.dividend.model.Auth;
import zerobase.dividend.model.MemberEntity;
import zerobase.dividend.security.TokenProvider;
import zerobase.dividend.service.MemberService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    private final TokenProvider tokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<Object> signup(@RequestBody Auth.SignUp request) {
        // 회원가입을 위한 API
        MemberEntity result = this.memberService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signin")
    public ResponseEntity<Object> signin(@RequestBody Auth.SignIn request) {
        // 로그인을 위한 API
        // password 검증
        MemberEntity member = this.memberService.authenticate(request);

        // 토큰 생성
        String token = this.tokenProvider.generateToken(member.getUsername(), member.getRoles());

        log.info("user login -> " + request.getUsername());
        return ResponseEntity.ok(token);
    }
}
