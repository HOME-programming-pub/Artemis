/*
 * This file configures the actual build steps for the automatic grading.
 *
 * !!!
 * For regular exercises, there is no need to make changes to this file.
 * Only this base configuration is actively supported by the Artemis maintainers
 * and/or your Artemis instance administrators.
 * !!!
 */

dockerImage = "#dockerImage"
dockerFlags = ""

/**
 * Main function called by Jenkins.
 */
void testRunner() {
    docker.image(dockerImage).inside(dockerFlags) { c ->
        runTestSteps()
    }
}

private void runTestSteps() {
    test()
}

/**
 * Run unit tests
 */
private void test() {
    stage('Build') {
        sh '''
        rm -rf Sources
        mv assignment/Sources .
        rm -rf assignment
        mkdir assignment
        cp -R Sources assignment
        cp -R Tests assignment
        cp Package.swift assignment

        # swift build
        cd assignment
        swift build

        # swift test
        swift test || true
        '''
    }
}

private void staticCodeAnalysis() {
    stage('Static Code Analysis') {
        sh '''
        rm -rf staticCodeAnalysisReports
        mkdir staticCodeAnalysisReports
        cp .swiftlint.yml assignment || true
        swiftlint lint assignment > staticCodeAnalysisReports/swiftlint-result.xml
        '''
    }
}

/**
 * Script of the post build tasks aggregating all JUnit files in $WORKSPACE/results.
 *
 * Called by Jenkins.
 */
void postBuildTasks() {
    if (#staticCodeAnalysisEnabled) {
        catchError {
            staticCodeAnalysis()
        }
    }

    sh '''
    rm -rf results
    mkdir results
    if [ -e assignment/tests.xml ]
    then
        sed -i 's/<testsuites>//g ; s/<\\/testsuites>//g' assignment/tests.xml
        cp assignment/tests.xml $WORKSPACE/results/ || true
    fi
    '''
}

// very important, do not remove
// required so that Jenkins finds the methods defined in this script
return this
