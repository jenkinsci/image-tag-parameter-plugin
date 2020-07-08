package io.jenkins.plugins.luxair.util;

import javax.annotation.Nonnull;

public class AuthService {
    private AuthType authType;
    private String realm;
    private String service;

    public AuthService(@Nonnull AuthType authType) {
        this(authType, null, null);
    }

    public AuthService(@Nonnull AuthType authType, String realm, String service) {
        this.authType = authType;
        this.realm = realm;
        this.service = service;
    }

    @Nonnull
    public AuthType getAuthType() {
        return authType;
    }

    public String getRealm() {
        return realm;
    }

    public String getService() {
        return service;
    }

    public void setAuthType(@Nonnull AuthType authType) {
        this.authType = authType;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "AuthService: " +
            "type=" + authType + ", " +
            "realm=" + realm + ", " +
            "service=" + service;
    }
}
