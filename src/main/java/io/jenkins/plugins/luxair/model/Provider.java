package io.jenkins.plugins.luxair.model;

public enum Provider {
    DOCKER_HUB("Docker Hub"),
    AWS_ECR("AWS ECR");
    public final String value;
    Provider(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
