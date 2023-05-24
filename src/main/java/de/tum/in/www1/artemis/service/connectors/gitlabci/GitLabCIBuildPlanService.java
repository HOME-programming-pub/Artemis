package de.tum.in.www1.artemis.service.connectors.gitlabci;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.domain.enumeration.ProjectType;
import de.tum.in.www1.artemis.service.ResourceLoaderService;

@Service
@Profile("gitlabci")
public class GitLabCIBuildPlanService {

    private static final Logger log = LoggerFactory.getLogger(GitLabCIBuildPlanService.class);

    private static final String FILE_NAME = ".gitlab-ci.yml";

    private final ResourceLoaderService resourceLoaderService;

    public GitLabCIBuildPlanService(ResourceLoaderService resourceLoaderService) {
        this.resourceLoaderService = resourceLoaderService;
    }

    /**
     * Generate the default build plan for the project type of the given programming
     * exercise.
     *
     * @param programmingExercise the programming exercise for which to get the
     *                                build plan
     * @return the default build plan
     */
    public String generateDefaultBuildPlan(ProgrammingExercise programmingExercise) {
        ProgrammingLanguage programmingLanguage = programmingExercise.getProgrammingLanguage();
        boolean isSequentialTestRuns = programmingExercise.hasSequentialTestRuns();
        boolean staticCodeAnalysisEnabled = programmingExercise.isStaticCodeAnalysisEnabled();

        final Optional<String> projectTypeName = getProjectTypeName(programmingLanguage, Optional.of(programmingExercise.getProjectType()));
        final Path resourcePath = buildResourcePath(programmingLanguage, projectTypeName, isSequentialTestRuns, staticCodeAnalysisEnabled);
        final Resource resource = resourceLoaderService.getResource(resourcePath);

        try {
            return StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        }
        catch (IOException ex) {
            log.error("Error loading template GitLab CI build configuration", ex);
            throw new IllegalStateException("Error loading template GitLab CI build configuration", ex);
        }
    }

    private static Optional<String> getProjectTypeName(final ProgrammingLanguage programmingLanguage, final Optional<ProjectType> projectType) {
        // Set a project type name in case the chosen GitLabCI File also depend on the
        // project type
        if (projectType.isPresent() && ProgrammingLanguage.C.equals(programmingLanguage)) {
            return Optional.of(projectType.get().name().toLowerCase(Locale.ROOT));
        }
        else if (projectType.isPresent() && projectType.get().isGradle()) {
            return Optional.of("gradle");
        }
        // Maven is also the project type for all other Java exercises (also if the
        // project type is not present)
        else if (ProgrammingLanguage.JAVA.equals(programmingLanguage)) {
            return Optional.of("maven");
        }
        else {
            return Optional.empty();
        }
    }

    private static Path buildResourcePath(final ProgrammingLanguage programmingLanguage, final Optional<String> projectTypeName, final boolean isSequentialRuns,
            final boolean isStaticCodeAnalysisEnabled) {
        final String programmingLanguageName = programmingLanguage.name().toLowerCase();
        final String regularOrSequentialDir = isSequentialRuns ? "sequentialRuns" : "regularRuns";
        final String pipelineScriptFilename = isStaticCodeAnalysisEnabled ? "staticCodeAnalysis" + FILE_NAME : FILE_NAME;

        Path resourcePath = Path.of("templates", "gitlabci", programmingLanguageName);
        if (projectTypeName.isPresent()) {
            resourcePath = resourcePath.resolve(projectTypeName.get());
        }

        return resourcePath.resolve(regularOrSequentialDir).resolve(pipelineScriptFilename);
    }
}
