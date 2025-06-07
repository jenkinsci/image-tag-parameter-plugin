package io.jenkins.plugins.luxair;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.Extension;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.logging.Logger;

@Extension
public class ImageTagParameterConfiguration extends GlobalConfiguration {

    private static final Logger logger = Logger.getLogger(ImageTagParameterConfiguration.class.getName());
    private static final String DEFAULT_REGISTRY = "https://registry-1.docker.io";

    public static ImageTagParameterConfiguration get() {
        return GlobalConfiguration.all().get(ImageTagParameterConfiguration.class);
    }

    private String defaultRegistry = DEFAULT_REGISTRY;
    private String defaultCredentialId = "";
    private Ordering defaultTagOrdering = Ordering.NATURAL;
    private boolean defaultVerifySsl = true;

    public ImageTagParameterConfiguration() {
        load();
    }

    public String getDefaultRegistry() {
        return StringUtil.isNotNullOrEmpty(defaultRegistry) ? defaultRegistry : DEFAULT_REGISTRY;
    }

    public String getDefaultCredentialId() {
        return StringUtil.isNotNullOrEmpty(defaultCredentialId) ? defaultCredentialId : "";
    }

    public Ordering getDefaultTagOrdering() {
        return defaultTagOrdering != null ? defaultTagOrdering : Ordering.NATURAL;
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) {
        if (json.has("defaultRegistry")) {
            this.defaultRegistry = json.getString("defaultRegistry");
            logger.fine("Changed default registry to: " + defaultRegistry);
        }
        if (json.has("defaultCredentialId")) {
            this.defaultRegistry = json.getString("defaultCredentialId");
            logger.fine("Changed default registry credentialsId to: " + defaultCredentialId);
        }
        if (json.has("defaultTagOrdering")) {
            this.defaultTagOrdering = Ordering.valueOf(json.getString("defaultTagOrdering"));
            logger.fine("Changed default tag ordering to: " + defaultTagOrdering);
        }
        save();
        return true;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultRegistry(String defaultRegistry) {
        logger.info("Changing default registry to: " + defaultRegistry);
        this.defaultRegistry = defaultRegistry;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultCredentialId(String defaultCredentialId) {
        logger.info("Changing default registry credentialsId to: " + defaultCredentialId);
        this.defaultCredentialId = defaultCredentialId;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultTagOrdering(Ordering defaultTagOrdering) {
        logger.info("Changing default tag ordering to: " + defaultTagOrdering);
        this.defaultTagOrdering = defaultTagOrdering;
        save();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillDefaultCredentialIdItems(@QueryParameter String credentialsId) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            logger.info("No permission to list credential");
            return new StandardListBoxModel().includeCurrentValue(defaultCredentialId);
        }
        return new StandardListBoxModel()
            .includeEmptyValue()
            .includeAs(ACL.SYSTEM, Jenkins.get(), StandardUsernameCredentials.class)
            .includeCurrentValue(defaultCredentialId);
    }

}