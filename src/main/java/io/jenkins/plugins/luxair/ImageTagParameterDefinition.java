package io.jenkins.plugins.luxair;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class ImageTagParameterDefinition extends SimpleParameterDefinition {

    private static final Logger logger = Logger.getLogger(ImageTagParameterDefinition.class.getName());
    private static final ImageTagParameterConfiguration config = ImageTagParameterConfiguration.get();

    private final String image;
    private final String registry;
    private final String filter;
    private final String credentialId;
    private final String defaultValue;
    private final Boolean returnOnlyTags;

    @DataBoundConstructor
    public ImageTagParameterDefinition(String name, String description, String image, String registry, String defaultValue, String filter, String credentialId, boolean returnOnlyTags) {
        super(name, description);
        this.image = image;
        this.registry = StringUtil.isNotNullOrEmpty(registry) ? registry : config.getDefaultRegistry();
        this.filter = StringUtil.isNotNullOrEmpty(filter) ? filter : ".*";
        this.credentialId = StringUtil.isNotNullOrEmpty(credentialId) ? credentialId : "";
        this.defaultValue = defaultValue;
        this.returnOnlyTags = returnOnlyTags;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public List<String> getTags() {
        List<String> imageTags;
        String user = "";
        String password = "";

        StandardUsernamePasswordCredentials credential = findCredential(credentialId);
        if (credential != null) {
            user = credential.getUsername();
            password = credential.getPassword().getPlainText();
        }
        imageTags = ImageTag.getTags(image, registry, filter, user, password);
        return imageTags;
    }

    public Boolean getReturnOnlyTags() {
        return returnOnlyTags;

    private StandardUsernamePasswordCredentials findCredential(String credentialId) {
        if (StringUtil.isNotNullOrEmpty(credentialId)) {
            List<Item> items = Jenkins.get().getAllItems();
            for (Item item : items) {
                List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    item,
                    ACL.SYSTEM,
                    Collections.emptyList());
                for (StandardUsernamePasswordCredentials cred : creds) {
                    if (cred.getId().equals(credentialId)) {
                        return cred;
                    }
                }
            }
            logger.warning("Cannot find credential for :" + credentialId + ":");
        } else {
            logger.info("CredentialId is empty");
        }
        return null;
    }

    private static final long serialVersionUID = 3938123092372L;

    @Override
    public ParameterValue createValue(String value) {
        return new StringParameterValue(getName(), value, getDescription());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return req.bindJSON(StringParameterValue.class, jo);
    }
    @Symbol("imageTag")
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "Image Tag Parameter";
        }

        public String defaultRegistry() {
            return config.getDefaultRegistry();
        }

        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialId,
                                                    @QueryParameter String registry) {
            if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) ||
                context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                logger.info("No permission to list credential");
                return new StandardListBoxModel().includeCurrentValue(credentialId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM, context, StandardUsernameCredentials.class)
                    .includeCurrentValue(credentialId);
        }
    }
}