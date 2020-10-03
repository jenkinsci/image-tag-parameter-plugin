package io.jenkins.plugins.luxair.util;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.luxair.Messages;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class CredentialsUtils {

    private static final Logger log = Logger.getLogger(CredentialsUtils.class.getName());

    private CredentialsUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static ListBoxModel doFillCredentialsIdItems(final Item context,
                                                        final String credentialsId) {
        if ((context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER))
            || (context != null
            && !context.hasPermission(Item.EXTENDED_READ)
            && !context.hasPermission(CredentialsProvider.USE_ITEM))) {
            log.info(Messages.ITP_CredentialsUtils_Log_info_NoPermission());
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
        }
        return new StandardListBoxModel()
            .includeEmptyValue()
            .includeMatchingAs(
                context instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) context) : ACL.SYSTEM,
                context,
                StandardCredentials.class,
                Collections.emptyList(),
                CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)))
            .includeCurrentValue(credentialsId);
    }

    public static FormValidation doCheckCredentialsId(final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return FormValidation.ok();
        }
        if (credentialsId.startsWith("${") && credentialsId.endsWith("}")) {
            return FormValidation.warning(Messages.ITP_CredentialsUtils_Form_warn_ExpressionBased());
        }
        if (!findCredentials(credentialsId).isPresent()) {
            return FormValidation.error(Messages.ITP_CredentialsUtils_Form_error_CannotFind());
        }
        return FormValidation.ok();
    }

    public static Optional<StandardUsernamePasswordCredentials> findCredentials(final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return Optional.empty();
        }
        List<StandardUsernamePasswordCredentials> lookupCredentials =
            CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                (Item) null,
                ACL.SYSTEM,
                Collections.emptyList());
        CredentialsMatcher allOf = CredentialsMatchers.allOf(
            CredentialsMatchers.withId(credentialsId),
            CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));
        return Optional.ofNullable(CredentialsMatchers.firstOrNull(lookupCredentials, allOf));
    }
}
