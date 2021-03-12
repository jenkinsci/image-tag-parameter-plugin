package io.jenkins.plugins.luxair.model;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class ResultContainer<V> {
    private List<String> errorMsgs = null;
    private V value;

    public ResultContainer(V defaultValue) {
        this.value = defaultValue;
    }

    public void addErrorMsg(String errorMsg) {
        if (this.errorMsgs == null) {
            this.errorMsgs = new ArrayList<String>();
        }
        this.errorMsgs.add(errorMsg);
    }

    public Optional<List<String>> getErrorMsgs() {
        return Optional.ofNullable(this.errorMsgs);
    }

    public void setErrorMsgs(List<String> errorMsgs) {
        this.errorMsgs = errorMsgs;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }
}
