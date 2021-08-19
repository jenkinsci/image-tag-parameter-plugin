package io.jenkins.plugins.luxair;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.util.CredentialsUtils;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
    public boolean configure(StaplerRequest req, JSONObject json) {
        if (json.has("defaultRegistry")) {
            this.defaultRegistry = json.getString("defaultRegistry");
            logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeRegistry(defaultRegistry));
        }
        if (json.has("defaultCredentialId")) {
            this.defaultRegistry = json.getString("defaultCredentialId");
            logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeCredential(defaultCredentialId));
        }
        if (json.has("defaultTagOrdering")) {
            this.defaultTagOrdering = Ordering.valueOf(json.getString("defaultTagOrdering"));
            logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeTagOrder(defaultTagOrdering));
        }
        save();
        return true;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultRegistry(String defaultRegistry) {
        logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeRegistry(defaultRegistry));
        this.defaultRegistry = defaultRegistry;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultCredentialId(String defaultCredentialId) {
        logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeCredential(defaultCredentialId));
        this.defaultCredentialId = defaultCredentialId;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultTagOrdering(Ordering defaultTagOrdering) {
        logger.fine(Messages.ITP_GlobalConfiguration_Log_fine_ChangeTagOrder(defaultTagOrdering));
        this.defaultTagOrdering = defaultTagOrdering;
        save();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillDefaultCredentialIdItems(@QueryParameter String credentialsId) {
        return CredentialsUtils.doFillCredentialsIdItems(null, credentialsId);
    }

    public FormValidation doCheckDefaultCredentialId(@QueryParameter String credentialsId) {
        return CredentialsUtils.doCheckCredentialsId(credentialsId);
    }
}