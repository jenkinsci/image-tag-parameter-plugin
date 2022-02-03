package io.jenkins.plugins.luxair;

import hudson.util.VersionNumber;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.model.ResultContainer;
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

    public static ResultContainer<List<String>> getTags(String image, String registry, String filter,
                                                        String user, String password, Ordering ordering, boolean verifySsl) {
        ResultContainer<List<String>> container = new ResultContainer<>(Collections.emptyList());

        String[] authService = getAuthService(registry);
        String token = getAuthToken(authService, image, user, password);
        ResultContainer<List<VersionNumber>> tags = getImageTagsFromRegistry(image, registry, authService[0], token, verifySsl);

        if (tags.getErrorMsg().isPresent()) {
            container.setErrorMsg(tags.getErrorMsg().get());
            return container;
        }

        ResultContainer<List<String>> filterTags = filterTags(tags.getValue(), filter, ordering);
        filterTags.getErrorMsg().ifPresent(container::setErrorMsg);
        container.setValue(filterTags.getValue());
        return container;
    }

    private static ResultContainer<List<String>> filterTags(List<VersionNumber> tags, String filter, Ordering ordering) {
        ResultContainer<List<String>> container = new ResultContainer<>(Collections.emptyList());
        logger.info("Ordering Tags according to: " + ordering);

        if (ordering == Ordering.DSC_VERSION || ordering == Ordering.ASC_VERSION) {
            try {
                container.setValue(tags.stream()
                   .filter(tag -> tag.toString().matches(filter))
                   .sorted(ordering == Ordering.ASC_VERSION ? VersionNumber::compareTo : VersionNumber.DESCENDING)
                   .map(VersionNumber::toString)
                   .collect(Collectors.toList()));
            } catch (Exception ignore) {
                logger.warning("Unable to cast ImageTags to versions! Versioned Ordering is not supported for this images tags.");
                container.setErrorMsg("Unable to cast ImageTags to versions! Versioned Ordering is not supported for this images tags.");
            }
        } else {
            container.setValue(tags.stream()
               .map(VersionNumber::toString)
               .filter(tag -> tag.matches(filter))
               .sorted(ordering == Ordering.NATURAL ? Collections.reverseOrder() : String::compareTo)
               .collect(Collectors.toList()));
        }

        return container;
    }

    private static String[] getAuthService(String registry) {

        String[] rtn = new String[3];
        rtn[0] = ""; // type
        rtn[1] = ""; // realm
        rtn[2] = ""; // service
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

        if (type.equalsIgnoreCase("Basic")) {
            rtn[0] = "Basic";
            logger.info("AuthService: type=Basic");

            return rtn;
        }

        if (type.equalsIgnoreCase("Bearer")) {
            String pattern = "Bearer realm=\"(\\S+)\",service=\"([\\S ]+)\"";
            Matcher m = Pattern.compile(pattern).matcher(headerValue);
            if (m.find()) {
                rtn[0] = "Bearer";
                rtn[1] = m.group(1);
                rtn[2] = m.group(2);
                logger.info("AuthService: type=Bearer, realm=" + rtn[0] + ", service=" + rtn[1]);
            } else {
                logger.warning("No AuthService available from " + url);
            }

            return rtn;
        }

        // Ops!
        logger.warning("Unknown authorization type " + type);

        return rtn;
    }

    private static String getAuthToken(String[] authService, String image, String user, String password) {

        String type = authService[0];
        String token = "";

        if (type.equals("Basic")) {
          // The password from the AWS ECR Plugin already is converted to the basic auth token
          if (user.equals("AWS")) {
            return password;
          } else {
            token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));

            return token;
          }
        }

        String realm = authService[1];
        String service = authService[2];

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        GetRequest request = Unirest.get(realm);
        if (!user.isEmpty() && !password.isEmpty()) {
            logger.info("Basic authentication");
            request = request.basicAuth(user, password);
        } else {
            logger.info("No basic authentication");
        }
        HttpResponse<JsonNode> response = request
            .queryString("service", service)
            .queryString("scope", "repository:" + image + ":pull")
            .asJson();
        if (response.isSuccess()) {
            JSONObject jsonObject = response.getBody().getObject();
            if (jsonObject.has("token")) {
                token = jsonObject.getString("token");
            } else if (jsonObject.has("access_token")) {
                token = jsonObject.getString("access_token");
            } else {
                logger.warning("Token not received");
            }
            logger.info("Token received");
        } else {
            logger.warning("Token not received");
        }
        Unirest.shutDown();

        return token;
    }

    private static ResultContainer<List<VersionNumber>> getImageTagsFromRegistry(String image, String registry,
                                                                                 String authType, String token, boolean verifySsl) {
        ResultContainer<List<VersionNumber>> resultContainer = new ResultContainer<>(new ArrayList<>());
        String url = registry + "/v2/" + image + "/tags/list";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        Unirest.config().verifySsl(verifySsl);
        HttpResponse<JsonNode> response = Unirest.get(url)
            .header("Authorization", authType + " " + token)
            .asJson();
        if (response.isSuccess()) {
            logger.info("HTTP status: " + response.getStatusText());
            response.getBody().getObject()
                .getJSONArray("tags")
                .forEach(item -> resultContainer.getValue().add(new VersionNumber(item.toString())));
        } else {
            logger.warning("HTTP status: " + response.getStatusText());
            resultContainer.setErrorMsg("HTTP status: " + response.getStatusText());
        }
        Unirest.shutDown();

        return resultContainer;
    }
}
