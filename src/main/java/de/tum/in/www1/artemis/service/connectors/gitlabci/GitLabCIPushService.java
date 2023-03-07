package de.tum.in.www1.artemis.service.connectors.gitlabci;

import org.eclipse.jgit.lib.Repository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationPushService;

@Service
@Profile("gitlab")
public class GitLabCIPushService implements ContinuousIntegrationPushService {

    /**
     * Trigger a build and process the result.
     *
     * @param commitHash the hash of the commit that was pushed.
     * @param repository the repository that was pushed to.
     */
    @Override
    public void processNewPush(String commitHash, Repository repository) {
        // Not needed for GitLab. The build is processed in GitLab and the CI system is notified via a webhook.
    }
}