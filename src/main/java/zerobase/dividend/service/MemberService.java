package zerobase.dividend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerobase.dividend.model.Auth;
import zerobase.dividend.model.MemberEntity;
import zerobase.dividend.repository.MemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));
    }

    public MemberEntity register(Auth.SignUp request) {
        boolean exists = this.memberRepository.existsByUsername(request.getUsername());
        if (exists) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        MemberEntity result = this.memberRepository.save(request.toEntity());

        return result;
    }

    // Password 인증
    public MemberEntity authenticate(Auth.SignIn request) {
        MemberEntity user = this.memberRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 ID 입니다"));

        // 인코딩되어 가입했던 비밀번호와 인코딩되지 않은 사용자 로그인 비밀번호 비교
        // requst의 password도 인코딩하여 user.password와 비교
        if (!this.passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        return user;
    }
}
