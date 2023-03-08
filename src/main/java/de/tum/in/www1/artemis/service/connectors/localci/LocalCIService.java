package de.tum.in.www1.artemis.service.connectors.localci;

import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.SOLUTION;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.TEMPLATE;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.VcsRepositoryUrl;
import de.tum.in.www1.artemis.domain.enumeration.BuildPlanType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.BambooException;
import de.tum.in.www1.artemis.exception.LocalCIException;
import de.tum.in.www1.artemis.repository.BuildLogStatisticsEntryRepository;
import de.tum.in.www1.artemis.repository.FeedbackRepository;
import de.tum.in.www1.artemis.repository.ProgrammingSubmissionRepository;
import de.tum.in.www1.artemis.service.BuildLogEntryService;
import de.tum.in.www1.artemis.service.connectors.AbstractContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.CIPermission;
import de.tum.in.www1.artemis.service.connectors.ConnectorHealth;
import de.tum.in.www1.artemis.service.hestia.TestwiseCoverageService;

@Service
@Profile("localci")
public class LocalCIService extends AbstractContinuousIntegrationService {

    private final Logger log = LoggerFactory.getLogger(LocalCIService.class);

    private final LocalCITriggerService localCITriggerService;

    public LocalCIService(ProgrammingSubmissionRepository programmingSubmissionRepository, FeedbackRepository feedbackRepository, BuildLogEntryService buildLogService,
            TestwiseCoverageService testwiseCoverageService, BuildLogStatisticsEntryRepository buildLogStatisticsEntryRepository, LocalCITriggerService localCITriggerService) {
        super(programmingSubmissionRepository, feedbackRepository, buildLogService, buildLogStatisticsEntryRepository, testwiseCoverageService);
        this.localCITriggerService = localCITriggerService;
    }

    @Override
    public void createBuildPlanForExercise(ProgrammingExercise programmingExercise, String planKey, VcsRepositoryUrl sourceCodeRepositoryURL, VcsRepositoryUrl testRepositoryURL,
            VcsRepositoryUrl solutionRepositoryURL) {
        // For Bamboo and Jenkins, this method is called for the template and the solution repository and creates and publishes a new build plan which results in a new build being
        // triggered.
        // For local CI, a build plan must not be created, because all the information for building a submission and running tests is contained in the participation.
        // However, we trigger a build for the template and solution participation here so the creator of an exercise gets build results for both right after creating the exercise.
        if (planKey.equals(TEMPLATE.getName())) {
            localCITriggerService.triggerBuild(programmingExercise.getTemplateParticipation());
        }
        else if (planKey.equals(SOLUTION.getName())) {
            localCITriggerService.triggerBuild(programmingExercise.getSolutionParticipation());
        }
    }

    @Override
    public void recreateBuildPlansForExercise(ProgrammingExercise exercise) {
        deleteBuildPlan(exercise.getProjectKey(), exercise.getTemplateBuildPlanId());
        deleteBuildPlan(exercise.getProjectKey(), exercise.getSolutionBuildPlanId());
        createBuildPlanForExercise(exercise, BuildPlanType.TEMPLATE.getName(), exercise.getVcsTemplateRepositoryUrl(), exercise.getVcsTestRepositoryUrl(),
                exercise.getVcsSolutionRepositoryUrl());
        createBuildPlanForExercise(exercise, BuildPlanType.SOLUTION.getName(), exercise.getVcsSolutionRepositoryUrl(), exercise.getVcsTestRepositoryUrl(),
                exercise.getVcsSolutionRepositoryUrl());
    }

    @Override
    public void configureBuildPlan(ProgrammingExerciseParticipation participation, String branch) {
        // Empty implementation. Not needed for local CI.
    }

    @Override
    public void performEmptySetupCommit(ProgrammingExerciseParticipation participation) {
        // Not needed for local CI. Implemented for Bamboo as a bug workaround.
    }

    @Override
    public void createProjectForExercise(ProgrammingExercise programmingExercise) {
        // Not needed for local CI. Build plans are grouped into projects automatically.
    }

    @Override
    public void deleteBuildPlan(String projectKey, String buildPlanId) {

        var buildPlan = getBuildPlan(buildPlanId);
        if (buildPlan == null) {
            log.error("Cannot delete {}, because it does not exist!", buildPlanId);
            return;
        }

        executeDelete("selectedBuilds", buildPlanId);
        log.info("Delete bamboo build plan {} was successful.", buildPlanId);
    }

    /**
     * NOTE: the REST call in this method fails silently with a 404 in case all build plans have already been deleted before
     *
     * @param projectKey the project which build plans should be retrieved
     * @return a list of build plans
     */
    private List<LocalCIBuildPlan> getBuildPlans(String projectKey) {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
        return List.of();
    }

    /**
     * Delete project with given identifier.
     *
     * @param projectKey unique identifier for the project on CI system
     */
    @Override
    public void deleteProject(String projectKey) {
        log.info("Trying to delete local CI project {}", projectKey);

        // TODO: check if the project actually exists, if not, we can immediately return

        // in normal cases this list should be empty, because all build plans have been deleted before
        final List<LocalCIBuildPlan> buildPlans = getBuildPlans(projectKey);
        for (var buildPlan : buildPlans) {
            try {
                deleteBuildPlan(projectKey, buildPlan.key());
            }
            catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }

        executeDelete("selectedProjects", projectKey);
        log.info("Deleting local CI project {} was successful.", projectKey);
    }

    private void executeDelete(String elementKey, String elementValue) {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
    }

    /**
     * Get the current status of the build for the given participation, i.e. INACTIVE, QUEUED, or BUILDING.
     *
     * @param participation participation for which to get status
     * @return build status
     */
    @Override
    public BuildStatus getBuildStatus(ProgrammingExerciseParticipation participation) {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
        return BuildStatus.INACTIVE;
    }

    /**
     * get the build plan for the given planKey
     *
     * @param planKey the unique local CI build plan identifier
     * @return the build plan
     */
    private LocalCIBuildPlan getBuildPlan(String planKey) {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
        return null;
    }

    @Override
    public String copyBuildPlan(String sourceProjectKey, String sourcePlanName, String targetProjectKey, String targetProjectName, String targetPlanName,
            boolean targetProjectExists) {
        final String cleanPlanName = getCleanPlanName(targetPlanName);
        return targetProjectKey + "-" + cleanPlanName;
    }

    @Override
    public void givePlanPermissions(ProgrammingExercise programmingExercise, String planName) {
        // Not implemented for local CI.
    }

    @Override
    public void removeAllDefaultProjectPermissions(String projectKey) {
        // Not implemented for local CI.
    }

    @Override
    public void giveProjectPermissions(String projectKey, List<String> groupNames, List<CIPermission> permissions) {
        // Not implemented for local CI.
    }

    @Override
    public void enablePlan(String projectKey, String planKey) throws LocalCIException {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
    }

    @Override
    public void updatePlanRepository(String buildProjectKey, String buildPlanKey, String ciRepoName, String repoProjectKey, String newRepoUrl, String existingRepoUrl,
            String newBranch, Optional<List<String>> optionalTriggeredByRepositories) throws LocalCIException {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
    }

    /**
     * Extract the plan key from the Bamboo requestBody.
     *
     * @param requestBody The request Body received from the CI-Server.
     * @return the plan key or null if it can't be found.
     * @throws BambooException is thrown on casting errors.
     */
    @Override
    public String getPlanKey(Object requestBody) throws LocalCIException {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
        return null;
    }

    @Override
    public ConnectorHealth health() {
        return new ConnectorHealth(true);
    }

    @Override
    public Optional<String> getWebHookUrl(String projectKey, String buildPlanId) {
        // No webhooks needed between local CI and local VC, so we return an empty Optional.
        return Optional.empty();
    }

    /**
     * Gets the latest available artifact for the given participation.
     *
     * @param participation to use its buildPlanId to find the artifact.
     * @return the html representation of the artifact page.
     */
    public ResponseEntity<byte[]> retrieveLatestArtifact(ProgrammingExerciseParticipation participation) {
        // TODO: Empty implementation to allow usage of 'localvc' with 'localci' in testing.
        return null;
    }

    @Override
    public String checkIfProjectExists(String projectKey, String projectName) {
        // TODO: Empty implementation to allow usage of local VCS with local CIS in testing.
        return "Not implemented yet.";
    }

    /**
     * Check if the given build plan is valid and accessible on the local CI system.
     *
     * @param buildPlanId unique identifier for build plan on CI system
     * @return true if the build plan exists.
     */
    @Override
    public boolean checkIfBuildPlanExists(String projectKey, String buildPlanId) {
        return getBuildPlan(buildPlanId.toUpperCase()) != null;
    }

    private String getCleanPlanName(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
