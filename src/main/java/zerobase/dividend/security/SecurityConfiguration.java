package zerobase.dividend.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) // hasRole을 어노테이션 방식으로 하기 위한 설정
@RequiredArgsConstructor
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .httpBasic().disable() // rest api로 서비스를 구현했기 때문에 사용하지 않는 부분 disable
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // jwt 토큰 방식은 stateless이므로 설정
            .and() // 권한제어
                .authorizeRequests()
                    .antMatchers("/**/signup","/**/signin").permitAll()
            .and() //
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 필터 순서 정하기

    }

    // 위쪽에 api 관련 권한 제어도 여기서 가능하지만 개발 부분만 따로 분리하기 위해서 위, 아래처럼 구분.
    // 모두 한 메소드에서 정해줘도 되지만 굳이?
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring() // 인증정보가 필요없는 경로들 설정
            .antMatchers("/h2-console/**");
    }

    // spring boot 2.x
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
