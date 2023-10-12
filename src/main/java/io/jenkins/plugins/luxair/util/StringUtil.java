package io.jenkins.plugins.luxair.util;

import hudson.Util;

public class StringUtil {

    public static boolean isNotNullOrEmpty(String param) {
        return param != null && !param.isEmpty();
    }

    public static String removeTrailingSlash(String input) {
        input = Util.fixNull(input).trim();

        while(input.endsWith("/")) {
            input = input.substring(0, input.length() - 1);
        }

        return input;
    }

}
