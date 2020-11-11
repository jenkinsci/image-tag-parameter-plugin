package io.jenkins.plugins.luxair;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.util.VariableResolver;
import io.jenkins.plugins.luxair.util.StringUtil;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.util.Locale;

/**
 * {@link ParameterValue} created from {@link ImageTagParameterDefinition}.
 */
public class ImageTagParameterValue extends ParameterValue {
    @Exported(visibility = 4)
    @Restricted(NoExternalUse.class)
    public String imageName;

    @Exported(visibility = 4)
    @Restricted(NoExternalUse.class)
    public String imageTag;

    @Exported(visibility = 4)
    @Restricted(NoExternalUse.class)
    public String ecrImageName;

    @Exported(visibility = 4)
    @Restricted(NoExternalUse.class)
    public String value;

    @DataBoundConstructor
    public ImageTagParameterValue(String name, String imageName, String imageTag, String ecrImageName) {
        this(name, imageName, imageTag, null, ecrImageName);
    }

    public ImageTagParameterValue(String name, String imageName, String imageTag, String description, String ecrImageName) {
        super(name, description);
        this.imageName = imageName;
        this.imageTag = imageTag;
        this.ecrImageName = ecrImageName;
        if(StringUtil.isNotNullOrEmpty(ecrImageName)) {
            this.value = String.format("%s:%s", ecrImageName, imageTag);
        } else {
            this.value = String.format("%s:%s", imageName, imageTag);
        }
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getEcrImageName() {
        return ecrImageName;
    }

    /**
     * Exposes the name/value as an environment variable.
     */
    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        // exposes ImageName
        env.put(String.format("%s_IMAGE", name), imageName);
        env.put(String.format("%s_IMAGE", name).toUpperCase(Locale.ENGLISH), imageName); // backward compatibility pre 1.345

        // exposes ImageTag
        env.put(String.format("%s_TAG", name), imageTag);
        env.put(String.format("%s_TAG", name).toUpperCase(Locale.ENGLISH), imageTag); // backward compatibility pre 1.345

        // exposes ImageName:ImageTag (aka. value)
        env.put(name, value);
        env.put(name.toUpperCase(Locale.ENGLISH), value); // backward compatibility pre 1.345
    }

    @Override
    public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
        return name -> ImageTagParameterValue.this.name.equals(name) ? value : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ImageTagParameterValue that = (ImageTagParameterValue) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "(ImageTagParameterValue) " + getName() + "='" + value + "'";
    }

    @Override
    public String getShortDescription() {
        return name + '=' + value;
    }
}
