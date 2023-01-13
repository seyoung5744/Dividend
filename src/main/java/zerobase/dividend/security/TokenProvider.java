package zerobase.dividend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // ms * s * m => 총 1시간

    private static final String KEY_ROLES = "roles";

    @Value("${spring.jwt.secret}")
    private String secretKey;

    /**
     * 토큰 생성(발급)
     *
     * @param username
     * @param roles
     * @return
     */
    public String generateToken(String username, List<String> roles) {
        // 사용자 권한 정보 저장을 위한 클래임
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        Date now = new Date();
        // 토큰 생성 시간 이후로 1시간 유효
        Date expiredDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now) // 토큰 생성 시간
            .setExpiration(expiredDate) // 토큰 만료 시간
            .signWith(SignatureAlgorithm.ES512, this.secretKey) // 사용할 암호화 알고리즘, 비밀키
            .compact();
    }

    // parsing한 정보로 유저 이름 가져옴.
    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        // 토큰이 빈 값이면 유효하지 않으므로 false.
        if (!StringUtils.hasText(token)) {
            return false;
        }

        Claims claims = this.parseClaims(token);

        // 만료 시간의 만료 시간이 현재 시간보다 이전인지 아닌지 판별
        return !claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        // 토큰 만료 시간이 경과된 이후로 parsing 하려고하면 ExpiredJwtException 발생
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            // TODO
            return e.getClaims();
        }
    }
}
