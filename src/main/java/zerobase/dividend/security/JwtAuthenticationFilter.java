package zerobase.dividend.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {// 한 요청 당 한번씩 필터 실행

    // 토큰은 http 프로토콜에서 header에 포함됨. 이때 어떤 key를 기준으로 토큰을 주고 받을지에 대한 key값
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    // 요청이 들어올 때마다 filter가 먼저 실행되면서 요청의 header에 토큰 유무를 확인하고 토큰이 유효하다면 인증 정보를 context에 담는다.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException
    {
        String token = this.resolveTokenFromRequest(request);

        // 요청이 들어올 때마다 filter에서 token 유효성 판단.
        if(StringUtils.hasText(token) && this.tokenProvider.validateToken(token)){
            // 토큰 유효성 검증
            Authentication auth = this.tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    // 1. request에 있는 header로부터 token 가져오기
    private String resolveTokenFromRequest(HttpServletRequest request){
        String token = request.getHeader(TOKEN_HEADER); // key 값으로 token 가져오기

        // 토큰이 유효한지는 모르겠지만
        // token이 존재하고 prefix로 시작하면 토큰 값이 존재하다고 판단하여 return
        if(!ObjectUtils.isEmpty(token) && token.startsWith(TOKEN_PREFIX)){
            return token.substring(TOKEN_PREFIX.length());
        }

        return null;
    }
}
