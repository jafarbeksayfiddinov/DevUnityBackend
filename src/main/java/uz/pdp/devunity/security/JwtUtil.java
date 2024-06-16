package uz.pdp.devunity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class JwtUtil {
    private final CustomUserDetailService customUserDetailService;

    public JwtUtil(CustomUserDetailService customUserDetailService) {
        this.customUserDetailService = customUserDetailService;
    }

    public String generateToken(String username) {
        UserDetails userDetails = customUserDetailService.loadUserByUsername(username);
        return Jwts.builder()
                .subject(username)
                .issuer("dev.uz")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24*7))
                .claim("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")))
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        StringBuilder value = getStringValueForSecretKey();
        byte[] bytes = Decoders.BASE64.decode(value.toString());
        return Keys.hmacShaKeyFor(bytes);
    }

    private static StringBuilder getStringValueForSecretKey() {
        StringBuilder value = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 64; i++) {
            value.append(random.nextInt(10));
        }
        return value;
    }

    public boolean isValid(String token) {
        Claims claims = getClaims(token);
        return true;
    }

    public String getUsername(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }


    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        Claims claims = getClaims(token);
        String str = claims.get("roles", String.class);
        String[] arr= str.split(",");
        return Arrays.stream(arr).map(item -> new SimpleGrantedAuthority(item)).toList();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
