package io.cahlee.auth.oauth2;

import io.cahlee.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final User.Provider provider;
    private final String providerId;

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName,
                                       Map<String, Object> attributes) {
        if ("github".equals(registrationId)) {
            return ofGithub(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .provider(User.Provider.GOOGLE)
                .providerId((String) attributes.get("sub"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuth2Attributes ofGithub(String userNameAttributeName, Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String displayName = (name != null && !name.isBlank()) ? name : login;
        String providerId = String.valueOf(attributes.get("id"));

        return OAuth2Attributes.builder()
                .name(displayName)
                .email(email)
                .provider(User.Provider.GITHUB)
                .providerId(providerId)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity() {
        return User.builder()
                .email(email)
                .username(name != null ? name : "User")
                .provider(provider)
                .providerId(providerId)
                .role(User.Role.USER)
                .build();
    }
}
