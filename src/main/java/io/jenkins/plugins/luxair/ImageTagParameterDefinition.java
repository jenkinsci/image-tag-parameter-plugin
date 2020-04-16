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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;


public class ImageTagParameterDefinition extends SimpleParameterDefinition {

    private static final Logger logger = Logger.getLogger(ImageTagParameterDefinition.class.getName());

    private String image;
    private String registry;
    private String filter;
    private String credentialId;

    @DataBoundConstructor
    public ImageTagParameterDefinition(String name, String description, String image, String registry, String filter, String credentialId) {
        super(name, description);
        this.image = image;
        this.registry = registry;
        if (filter.isEmpty()) {
            this.filter = ".*";
        } else {
            this.filter = filter;
        }
        this.credentialId = credentialId;
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

    public String getCredentialId() {
        return credentialId;
    }

    private StandardUsernamePasswordCredentials findCredential(String credentialId) {
        if (!credentialId.isEmpty()) {
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
            String defaultRegistry = ImageTagParameterConfiguration.get().getDefaultRegistry();
            if (defaultRegistry.isEmpty()) {
                defaultRegistry = "https://registry-1.docker.io";
                ImageTagParameterConfiguration.get().setDefaultRegistry(defaultRegistry);
            }
            return defaultRegistry;
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