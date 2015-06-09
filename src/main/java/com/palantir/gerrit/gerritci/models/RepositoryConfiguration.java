package com.palantir.gerrit.gerritci.models;


public class RepositoryConfiguration {
    private Integer repoId;
    private Boolean ciEnabled;
    private String verifyBranchRegex;
    private String publishBranchRegex;
    private JenkinsServerConfiguration jsc;

    public Integer getRepoId() {
        return repoId;
    }

    public void setRepoId(int repoId) {
        this.repoId = repoId;
    }

    public Boolean getCiEnabled() {
        return ciEnabled;
    }

    public void setCiEnabled(boolean ciEnabled) {
        this.ciEnabled = ciEnabled;
    }

    public String getVerifyBranchRegex() {
        return verifyBranchRegex;
    }

    public void setVerifyBranchRegex(String verifyBranchRegex) {
        this.verifyBranchRegex = verifyBranchRegex;
    }

    public String getPublishBranchRegex() {
        return publishBranchRegex;
    }

    public void setPublishBranchRegex(String publishBranchRegex) {
        this.publishBranchRegex = publishBranchRegex;
    }

    public JenkinsServerConfiguration getJenkinsServerConfiguration() {
        return jsc;
    }

    public void setJenkinsServerConfiguration(JenkinsServerConfiguration jsc) {
        this.jsc = jsc;
    }
}
