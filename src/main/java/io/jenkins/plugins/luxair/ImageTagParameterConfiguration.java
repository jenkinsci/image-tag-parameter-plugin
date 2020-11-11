package io.jenkins.plugins.luxair;

import com.amazonaws.regions.Regions;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.Extension;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.model.Provider;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
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
    private Provider defaultTagProvider = Provider.DOCKER_HUB;
    private Regions defaultTagAWSRegion = Regions.US_EAST_1;
    private String ecrImageName = "";

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

    public Provider getDefaultTagProvider() {
        return defaultTagProvider != null ? defaultTagProvider : Provider.DOCKER_HUB;
    }

    public Regions getDefaultTagAWSRegion() {
        return defaultTagAWSRegion != null ? defaultTagAWSRegion : Regions.US_EAST_1;
    }

    public String getEcrImageName() {
        return ecrImageName != null ? ecrImageName : "";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
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
        if (json.has("defaultTagProvider")) {
            this.defaultTagProvider = Provider.valueOf(json.getString("defaultTagProvider"));
            logger.fine("Changed default tag provider to: " + defaultTagProvider);
        }
        if (json.has("defaultTagAWSRegion")) {
            this.defaultTagAWSRegion = Regions.valueOf(json.getString("defaultTagAWSRegion"));
            logger.fine("Changed default tag aws region to: " + defaultTagAWSRegion);
        }
        if (json.has("ecrImageName")) {
            this.ecrImageName = json.getString("ecrImageName");
            logger.fine("Changed image name to: " + ecrImageName);
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

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultTagProvider(Provider defaultTagProvider) {
        logger.info("Changing default tag provider to: " + defaultTagProvider);
        this.defaultTagProvider = defaultTagProvider;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDefaultTagAWSRegion(Regions defaultTagAWSRegion) {
        logger.info("Changing default tag aws region to: " + defaultTagAWSRegion);
        this.defaultTagAWSRegion = defaultTagAWSRegion;
        save();
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setEcrImageName(String ecrImageName) {
        logger.info("Changing ecr image name to: " + ecrImageName);
        this.ecrImageName = ecrImageName;
        save();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillDefaultCredentialIdItems(@QueryParameter String credentialsId) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            logger.info("No permission to list credential");
            return new StandardListBoxModel().includeCurrentValue(defaultCredentialId);
        }
        ListBoxModel allCredentials = new ListBoxModel();
        allCredentials
            .addAll(AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.get()));
        allCredentials
            .addAll(new StandardListBoxModel()
            .includeEmptyValue()
            .includeAs(ACL.SYSTEM, Jenkins.get(), StandardUsernameCredentials.class)
            .includeCurrentValue(defaultCredentialId));
        return allCredentials;
    }

}