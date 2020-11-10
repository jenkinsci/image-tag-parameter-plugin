package io.jenkins.plugins.luxair;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.model.Ordering;
import io.jenkins.plugins.luxair.model.Provider;
import io.jenkins.plugins.luxair.model.ResultContainer;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


public class ImageTagParameterDefinition extends SimpleParameterDefinition {

    private static final long serialVersionUID = 3938123092372L;
    private static final Logger logger = Logger.getLogger(ImageTagParameterDefinition.class.getName());
    private static final ImageTagParameterConfiguration config = ImageTagParameterConfiguration.get();
    private final String AWS_REGISTRY = "%s.dkr.ecr.%s.amazonaws.com/%s";

    private final String image;
    private final String registry;
    private final String filter;
    private final String credentialId;
    private final String ecrImageName;

    private String defaultTag;
    private Ordering tagOrder;
    private String defaultProvider;
    private Provider tagProvider;
    private String defaultAWSRegion;
    private Regions tagAWSRegions;

    private String errorMsg = "";
    private String userId = "";

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public ImageTagParameterDefinition(String name, String description, String image, String filter,
                                       String registry, String credentialId, String ecrImageName) {
        this(name, description, image, filter, "","", registry, credentialId, config.getDefaultTagOrdering(), config.getDefaultTagProvider(), config.getDefaultTagAWSRegion(), config.getEcrImageName());
    }

    public ImageTagParameterDefinition(String name, String description, String image, String filter, String defaultTag, String defaultProvider,
                                       String registry, String credentialId, Ordering tagOrder, Provider tagProvider, Regions tagAWSRegions, String ecrImageName) {
        super(name, description);
        this.image = image;
        this.registry = StringUtil.isNotNullOrEmpty(registry) ? registry : config.getDefaultRegistry();
        this.filter = StringUtil.isNotNullOrEmpty(filter) ? filter : ".*";
        this.defaultTag = StringUtil.isNotNullOrEmpty(defaultTag) ? defaultTag : "";
        this.defaultProvider = StringUtil.isNotNullOrEmpty(defaultProvider) ? defaultProvider : "";
        this.defaultAWSRegion = StringUtil.isNotNullOrEmpty(defaultAWSRegion) ? defaultAWSRegion : "";
        this.credentialId = getDefaultOrEmptyCredentialId(this.registry, credentialId);
        this.tagOrder = tagOrder != null ? tagOrder : config.getDefaultTagOrdering();
        this.tagProvider = tagProvider != null ? tagProvider : config.getDefaultTagProvider();
        this.tagAWSRegions = tagAWSRegions != null ? tagAWSRegions : config.getDefaultTagAWSRegion();
        this.ecrImageName = StringUtil.isNotNullOrEmpty(ecrImageName) ? ecrImageName : config.getEcrImageName();
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

    public String getEcrImageName() { return ecrImageName; }

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

    public Provider getTagProvider() { return tagProvider; }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setTagProvider(Provider tagProvider) { this.tagProvider = tagProvider; }


    public Regions getTagAWSRegions() { return tagAWSRegions; }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setTagAWSRegions(Regions tagAWSRegions) {
        this.tagAWSRegions = tagAWSRegions;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public String getDefaultAWSRegion() {
        return defaultAWSRegion;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
        if(tagProvider.value == Provider.DOCKER_HUB.value) {
            String user = "";
            String password = "";

            StandardUsernamePasswordCredentials credential = findCredential(credentialId);
            if (credential != null) {
                user = credential.getUsername();
                password = credential.getPassword().getPlainText();
            }

            ResultContainer<List<String>> resultContainer = ImageTag.getTags(image, registry, filter, user, password, tagOrder);
            Optional<String> optionalErrorMsg = resultContainer.getErrorMsg();
            if (optionalErrorMsg.isPresent()) {
                setErrorMsg(optionalErrorMsg.get());
            } else {
                setErrorMsg("");
            }

            return resultContainer.getValue();
        }else if(tagProvider.value == Provider.AWS_ECR.value){
            AWSCredentials awsCredentials = findAWSCredentials(credentialId);
            ResultContainer<List<String>> resultContainer = new  ResultContainer<>(null);
            if(awsCredentials != null) {
                resultContainer = ImageTag.getAWSECRTags(image, tagAWSRegions.getName(), filter, awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey(), tagOrder);
                String userId = ImageTag.getAWSUserId(awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey());
                if(StringUtil.isNotNullOrEmpty(userId)) {
                    String ecrImageName = String.format(AWS_REGISTRY, userId, tagAWSRegions.getName(), image);
                    config.setEcrImageName(ecrImageName);

                    Optional<String> optionalErrorMsg = resultContainer.getErrorMsg();
                    if(optionalErrorMsg.isPresent()) {
                        setErrorMsg(optionalErrorMsg.get());
                    } else {
                        setErrorMsg("");
                    }
                } else {
                    setErrorMsg("Failed to load user ID from AWS");
                }
                return  resultContainer.getValue();
            } else {
                setErrorMsg("AWS credentials were not found");
                return  resultContainer.getValue();
            }
        }
        return null;
    }

    private AWSCredentials findAWSCredentials(String credentialId) {
        try {
            if(StringUtil.isNotNullOrEmpty(credentialId)) {
                return AWSCredentialsHelper.getCredentials(credentialId, Jenkins.get()).getCredentials();
            } else {
                logger.info("CredentialId is empty");
                return null;
            }
        } catch(Exception e) {
            logger.warning("Cannot find aws credential for :" + credentialId + ":");
            return null;
        }
    }

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

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof ImageTagParameterValue) {
            ImageTagParameterValue value = (ImageTagParameterValue) defaultValue;
            return new ImageTagParameterDefinition(getName(), getDescription(),
                getImage(), getFilter(), value.getImageTag(), getDefaultProvider(),
                getRegistry(), getCredentialId(), getTagOrder(), getTagProvider(), getTagAWSRegions(), getEcrImageName());
        }
        return this;
    }

    @Override
    public ParameterValue createValue(String value) {
        if (tagProvider.value == Provider.DOCKER_HUB.value) {
            return new ImageTagParameterValue(getName(), image, value, getDescription());
        } else if(tagProvider.value == Provider.AWS_ECR.value) {
            AWSCredentials awsCredentials = findAWSCredentials(credentialId);
            if(awsCredentials != null) {
                String userId = ImageTag.getAWSUserId(awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey());
                if(StringUtil.isNotNullOrEmpty(userId)) {
                    String ecrImageName = String.format(AWS_REGISTRY, userId, tagAWSRegions.getName(), image);
                    config.setEcrImageName(ecrImageName);
                    return new ImageTagParameterValue(getName(), image, value, getDescription(), ecrImageName);
                } else {
                    setErrorMsg("AWS User not found");
                }
            } else {
                setErrorMsg("Unable to find credentials");
            }
        }
        return null;
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
            return "Image Tag Parameter";
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
        public Provider getDefaultTagProvider() {
            return config.getDefaultTagProvider();
        }

        @SuppressWarnings("unused")
        public Regions getDefaultTagAWSRegion() {
            return config.getDefaultTagAWSRegion();
        }

        @SuppressWarnings("unused")
        public String getEcrImageName() {
            return config.getEcrImageName();
        }
        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialId) {
            if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) ||
                context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                logger.info("No permission to list credential");
                return new StandardListBoxModel().includeCurrentValue(credentialId);
            }
            ListBoxModel allCredentials = new ListBoxModel();
            allCredentials
                .addAll(AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.get()));
            allCredentials
                .addAll(new StandardListBoxModel()
                .includeEmptyValue()
                .includeAs(ACL.SYSTEM, context, StandardUsernameCredentials.class)
                .includeCurrentValue(credentialId));
            return allCredentials;
        }
    }
}