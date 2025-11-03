package com.deeplearningbasic.autograder.service;

import com.deeplearningbasic.autograder.config.AdminProperties;
import com.deeplearningbasic.autograder.domain.Role;
import com.deeplearningbasic.autograder.domain.User;
import com.deeplearningbasic.autograder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    public OidcUser loadUser(OidcUserRequest req) throws OAuth2AuthenticationException {
        OidcUser oidc = super.loadUser(req);
        Map<String, Object> attrs = new HashMap<>(oidc.getAttributes());

        String email = String.valueOf(attrs.getOrDefault("email", "")).trim();
        if (email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OIDC response");
        }

        if (!email.endsWith("@hufs.ac.kr")) {
            throw new OAuth2AuthenticationException("hufs.ac.kr 계정으로만 로그인할 수 있습니다.");
        }

        String name = Optional.ofNullable((String) attrs.get("name"))
                .orElseGet(() -> String.valueOf(attrs.getOrDefault("given_name", email))).trim();

        User user = saveOrUpdate(email, name);

        // 기존 권한 + ROLE_*
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidc.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())); // ROLE_ADMIN/ROLE_USER

        // email을 name attribute로 사용
        return new DefaultOidcUser(authorities, oidc.getIdToken(), oidc.getUserInfo(), "email");
    }

    private User saveOrUpdate(String email, String name) {
        Role role = adminProperties.getEmails().contains(email) ? Role.ADMIN : Role.USER;
        User user = userRepository.findByEmail(email)
                .map(u -> u.update(name, role))
                .orElse(User.builder()
                        .email(email)
                        .name(name)
                        .role(role)
                        .build());
        return userRepository.save(user);
    }
}