package com.palantir.gerrit.gerritci.constants;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Enumerates all distinct types of jobs that we will create in Jenkins for a given Gerrit
 * repository.
 */
public enum JobType {
    /**
     * Verify builds will typically run whenever a patchset is created or a draft published. They
     * are used to see if the changes caused any tests to fail.
     */
    VERIFY,

    /**
     * Publish builds will typically run when a change is merged. They will usually publish the
     * resultant artifact of the build to some repository.
     */
    PUBLISH,
    /**
     * Cron builds will run on a configured schedule
     */
    CRON;

    private static final Map<JobType, String> nameMap = ImmutableMap.of(VERIFY, "verify", PUBLISH,
        "publish", CRON, "cron");

    private static final Map<JobType, String> templateMap = ImmutableMap.of(VERIFY,
        "/jenkins-verify-job.vm", PUBLISH, "/jenkins-publish-job.vm", CRON, "/jenkins-cron-job.vm");

    /**
     * Given a JobType, gives the filename of the template file to use when creating new builds.
     *
     * @return filename of the template file for this type of JobType.
     */
    public String getTemplate() {
        return templateMap.get(this);
    }

    /**
     * Given a project name, returns the name of the Jenkins job associated with this job type.
     *
     * @param projectName The name of the project to get the job name for.
     * @return The name for the Jenkins job with the specified project name and this job type.
     */
    public String getJobName(String projectName) {
        return String.format("gerrit-ci_%s_%s", projectName.replace('/', '_'), nameMap.get(this));
    }

    @Override
    public String toString() {
        return nameMap.get(this);
    }
}