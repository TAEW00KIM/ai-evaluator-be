package com.deeplearningbasic.autograder.service;

import com.deeplearningbasic.autograder.config.AdminProperties;
import com.deeplearningbasic.autograder.domain.Role;
import com.deeplearningbasic.autograder.domain.User;
import com.deeplearningbasic.autograder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oauth2 = super.loadUser(req);
        Map<String, Object> attrs = new HashMap<>(oauth2.getAttributes());

        // 이메일은 필수
        final String email = String.valueOf(attrs.getOrDefault("email","")).trim();
        if (email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 response");
        }

        if (!email.endsWith("@hufs.ac.kr")) {
            throw new OAuth2AuthenticationException("hufs.ac.kr 계정으로만 로그인할 수 있습니다.");
        }

        // 이름은 name/given_name 중 하나 사용
        final String name = Optional.ofNullable((String) attrs.get("name"))
                .orElseGet(() -> String.valueOf(attrs.getOrDefault("given_name", email))).trim();

        User user = saveOrUpdate(email, name);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oauth2.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())); // ROLE_ADMIN or ROLE_USER

        // user-name-attribute 로 email 사용
        return new DefaultOAuth2User(authorities, attrs, "email");
    }

    private User saveOrUpdate(String email, String name) {
        // 관리자 이메일이면 ADMIN, 아니면 USER
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