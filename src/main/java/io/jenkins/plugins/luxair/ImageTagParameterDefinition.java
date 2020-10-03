package io.jenkins.plugins.luxair;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.model.ResultContainer;
import io.jenkins.plugins.luxair.util.CredentialsUtils;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;


public class ImageTagParameterDefinition extends SimpleParameterDefinition {

    private static final long serialVersionUID = 3938123092372L;
    private static final ImageTagParameterConfiguration config = ImageTagParameterConfiguration.get();

    private final String image;
    private final String registry;
    private final String filter;
    private final String credentialId;
    private String defaultTag;
    private Ordering tagOrder;
    private String errorMsg = "";

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public ImageTagParameterDefinition(String name, String description, String image, String filter,
                                       String registry, String credentialId) {
        this(name, description, image, filter, "", registry, credentialId, config.getDefaultTagOrdering());
    }

    public ImageTagParameterDefinition(String name, String description, String image, String filter, String defaultTag,
                                       String registry, String credentialId, Ordering tagOrder) {
        super(name, description);
        this.image = image;
        this.registry = StringUtil.isNotNullOrEmpty(registry) ? registry : config.getDefaultRegistry();
        this.filter = StringUtil.isNotNullOrEmpty(filter) ? filter : ".*";
        this.defaultTag = StringUtil.isNotNullOrEmpty(defaultTag) ? defaultTag : "";
        this.credentialId = getDefaultOrEmptyCredentialId(this.registry, credentialId);
        this.tagOrder = tagOrder != null ? tagOrder : config.getDefaultTagOrdering();
    }

    public String getImage() {
        return image;
    }

    public String getRegistry() {
        return registry;
    }

    public String getFilter() {
        return filter;
    }

    public String getDefaultTag() {
        return defaultTag;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultTag(String defaultTag) {
        this.defaultTag = defaultTag;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public Ordering getTagOrder() {
        return tagOrder;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setTagOrder(Ordering tagOrder) {
        this.tagOrder = tagOrder;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    private String getDefaultOrEmptyCredentialId(String registry, String credentialId) {
        if (registry.equals(config.getDefaultRegistry()) && !StringUtil.isNotNullOrEmpty(credentialId)) {
            return config.getDefaultCredentialId();
        } else if (StringUtil.isNotNullOrEmpty(credentialId)) {
            return credentialId;
        } else {
            return "";
        }
    }

    public List<String> getTags() {
        String user = "";
        String password = "";

        Optional<StandardUsernamePasswordCredentials> credential = CredentialsUtils.findCredentials(credentialId);
        if (credential.isPresent()) {
            user = credential.get().getUsername();
            password = credential.get().getPassword().getPlainText();
        }

        ResultContainer<List<String>> resultContainer = ImageTag.getTags(image, registry, filter, user, password, tagOrder);
        Optional<String> optionalErrorMsg = resultContainer.getErrorMsg();
        if (optionalErrorMsg.isPresent()) {
            setErrorMsg(optionalErrorMsg.get());
        } else {
            setErrorMsg("");
        }

        return resultContainer.getValue();
    }

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof ImageTagParameterValue) {
            ImageTagParameterValue value = (ImageTagParameterValue) defaultValue;
            return new ImageTagParameterDefinition(getName(), getDescription(),
                getImage(), getFilter(), value.getImageTag(),
                getRegistry(), getCredentialId(), getTagOrder());
        }
        return this;
    }

    @Override
    public ParameterValue createValue(String value) {
        return new ImageTagParameterValue(getName(), image, value, getDescription());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return req.bindJSON(ImageTagParameterValue.class, jo);
    }

    @Symbol("imageTag")
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.ITP_DescriptorImpl_DisplayName();
        }

        @SuppressWarnings("unused")
        public String getDefaultRegistry() {
            return config.getDefaultRegistry();
        }

        @SuppressWarnings("unused")
        public String getDefaultCredentialID() {
            return config.getDefaultCredentialId();
        }

        @SuppressWarnings("unused")
        public Ordering getDefaultTagOrdering() {
            return config.getDefaultTagOrdering();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialId) {
            return CredentialsUtils.doFillCredentialsIdItems(context, credentialId);
        }

        public FormValidation doCheckCredentialId(@QueryParameter final String value,
                                                  @AncestorInPath final Item context) {
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ) && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }

            return CredentialsUtils.doCheckCredentialsId(value);
        }
    }
}