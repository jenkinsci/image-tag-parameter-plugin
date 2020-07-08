package io.jenkins.plugins.luxair;

import hudson.util.VersionNumber;
import io.jenkins.plugins.luxair.util.AuthService;
import io.jenkins.plugins.luxair.util.AuthType;
import kong.unirest.*;
import kong.unirest.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ImageTag {

    private static final Logger logger = Logger.getLogger(ImageTag.class.getName());
    private static final Interceptor errorInterceptor = new ErrorInterceptor();

    private ImageTag() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> getTags(String image, String registry, String filter,
                                       String user, String password, boolean reverseOrdering) {
        AuthService authService = getAuthService(registry);
        if (authService.getAuthType() != AuthType.UNKNOWN) {
            String token = getAuthToken(authService, image, user, password);
            List<VersionNumber> tags = getImageTagsFromRegistry(image, registry, authService.getAuthType(), token);
            return tags.stream().filter(tag -> tag.toString().matches(filter))
                .sorted(!reverseOrdering ? VersionNumber.DESCENDING : VersionNumber::compareTo)
                .map(VersionNumber::toString)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static AuthService getAuthService(String registry) {
        AuthService authService = new AuthService(AuthType.UNKNOWN);
        String url = registry + "/v2/";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        String headerValue = Unirest.get(url).asEmpty()
            .getHeaders().getFirst("Www-Authenticate");
        Unirest.shutDown();

        String type = "";

        String typePattern = "^(\\S+)";
        Matcher typeMatcher = Pattern.compile(typePattern).matcher(headerValue);
        if (typeMatcher.find()) {
            type = typeMatcher.group(1);
        }

        if (type.equals(AuthType.BASIC.value)) {
            authService.setAuthType(AuthType.BASIC);
            logger.info("AuthService: type=Basic");
        } else if (type.equals(AuthType.BEARER.value)) {
            String pattern = "Bearer realm=\"(\\S+)\",service=\"(\\S+)\"";
            Matcher m = Pattern.compile(pattern).matcher(headerValue);
            if (m.find()) {
                authService.setAuthType(AuthType.BEARER);
                authService.setRealm(m.group(1));
                authService.setService(m.group(2));
                logger.info("AuthService: type=Bearer, realm=" + m.group(1) + ", service=" + m.group(2));
            } else {
                logger.warning("No AuthService available from " + url);
            }
        } else {
            logger.warning("Unknown authorization type! Received type: " + type);
        }

        return authService;
    }

    private static String getAuthToken(AuthService authService, String image, String user, String password) {
        String token = "";

        switch (authService.getAuthType()) {
            case BASIC:
                token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
                break;
            case BEARER:
                token = getBearerAuthToken(authService, image, user, password);
                break;
            default:
                logger.warning("AuthServiceType is unknown. Unable to fetch AuthToken.");
        }

        return token;
    }

    private static String getBearerAuthToken(AuthService authService, String image, String user, String password) {
        String token = "";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        GetRequest request = Unirest.get(authService.getRealm());
        if (!user.isEmpty() && !password.isEmpty()) {
            logger.info("Using Basic authentication to fetch AuthToken");
            request = request.basicAuth(user, password);
        }
        HttpResponse<JsonNode> response = request
            .queryString("service", authService.getService())
            .queryString("scope", "repository:" + image + ":pull")
            .asJson();
        if (response.isSuccess()) {
            token = findTokenInResponse(response, "token", "access_token");
        } else {
            logger.warning("Request failed! Token was not received");
        }
        Unirest.shutDown();

        return token;
    }

    private static String findTokenInResponse(HttpResponse<JsonNode> response, String... searchKey) {
        JSONObject jsonObj = response.getBody().getObject();

        for (String key : searchKey) {
            if (jsonObj.has(key)) {
                logger.info("Token received");
                return jsonObj.getString(key);
            }
        }

        logger.warning("Unable to find token in response! Token was not received");
        return "";
    }

    private static List<VersionNumber> getImageTagsFromRegistry(String image, String registry,
                                                                AuthType authType, String token) {
        List<VersionNumber> tags = new ArrayList<>();
        String url = registry + "/v2/{image}/tags/list";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        HttpResponse<JsonNode> response = Unirest.get(url)
            .header("Authorization", authType + " " + token)
            .routeParam("image", image)
            .asJson();
        if (response.isSuccess()) {
            logger.info("HTTP status: " + response.getStatusText());
            response.getBody().getObject()
                .getJSONArray("tags")
                .forEach(item -> tags.add(new VersionNumber(item.toString())));
        } else {
            logger.warning("Image tags request responded with HTTP status: " + response.getStatusText());
        }
        Unirest.shutDown();

        return tags;
    }
}