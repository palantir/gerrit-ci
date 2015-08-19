//   Copyright 2015 Palantir Technologies
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
// 
//       http://www.apache.org/licenses/LICENSE-2.0
// 
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   
//   See the License for the specific language governing permissions and
//   limitations under the License.
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
        "/jenkins-verify-job.xml", PUBLISH, "/jenkins-publish-job.xml", CRON, "/jenkins-cron-job.xml");

    private static final Map<JobType, String> templateMapNoJunit = ImmutableMap.of(VERIFY,
            "/jenkins-verify-job-nojunit.xml", PUBLISH, "/jenkins-publish-job-nojunit.xml", CRON, "/jenkins-cron-job-nojunit.xml");


    /**
     * Given a JobType, gives the filename of the template file to use when creating new builds.
     *
     * @return filename of the template file for this type of JobType.
     */
    public String getTemplate() {
        return templateMap.get(this);
    }

    /**
     * Given a JobType, gives the filename of the template file to use when creating new builds.
     *
     * @return filename of the template file for this type of JobType.
     */
    public String getTemplateNoJunit() {
        return templateMapNoJunit.get(this);
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