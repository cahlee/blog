package com.blog.auth.oauth2;

import com.blog.domain.user.User;
import com.blog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());

        String email = attributes.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not provided by OAuth2 provider. Please ensure your account has a public email.");
        }

        User user = saveOrUpdate(attributes);
        return new CustomOAuth2UserDetailsAdapter(user, oAuth2User.getAttributes(), attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuth2Attributes attributes) {
        Optional<User> existingUserOpt = userRepository.findByEmail(attributes.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getProvider() != attributes.getProvider()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_already_registered"),
                        "This email is already registered with a different provider: " + existingUser.getProvider().name());
            }
            existingUser.setUsername(attributes.getName() != null ? attributes.getName() : existingUser.getUsername());
            existingUser.setProviderId(attributes.getProviderId());
            return userRepository.save(existingUser);
        }

        return userRepository.save(attributes.toEntity());
    }
}
