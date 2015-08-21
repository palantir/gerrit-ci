package com.palantir.gerrit.gerritci.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.GerritConfig;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;

public class ConfigFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFileUtils.class);

    private ConfigFileUtils(){

    }

    public static JenkinsServerConfiguration getJenkinsConfigFromFile(File f){
        FileBasedConfig cfg = new FileBasedConfig(f, FS.DETECTED);
        try {
            cfg.load();
        } catch (ConfigInvalidException | IOException e) {
            logger.error("Error loading config file after get request:", e);
            return null;
        }

        String jenkinsUrlString = cfg.getString("Settings", "Jenkins", "jenkinsURL");
        String jenkinsUserString = cfg.getString("Settings", "Jenkins", "jenkinsUser");
        String jenkinsPasswordString = cfg.getString("Settings", "Jenkins", "jenkinsPassword");
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();

        try {
            jsc.setUri(new URI(jenkinsUrlString));
        } catch (Exception e) {
            logger.error("Error loading config file after get request:", e);
        }
        jsc.setUsername(jenkinsUserString);
        jsc.setPassword(jenkinsPasswordString);
        return jsc;
    }

    public static ArrayList<String> getJobsFromFile(File f) throws IOException {
        ArrayList<String> jobs = new ArrayList<String>();
        File projectConfigDirectory = f;
        if (!projectConfigDirectory.exists())
            projectConfigDirectory.mkdir();
        File projectConfigFile = new File(projectConfigDirectory, "created_jobs");
        if (!projectConfigFile.exists())
            projectConfigFile.createNewFile();
        Scanner scanner = new Scanner(projectConfigFile);
        while (scanner.hasNext()) {
            String line = scanner.next();
            jobs.add(line);
        }
        scanner.close();
        return jobs;
    }

    public static Map<String, Object> getJenkinsSpecificParams(File f, String jarPath, GerritConfig gerritConfig, String canonicalWebUrl) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        FileBasedConfig cfg = new FileBasedConfig(f, FS.DETECTED);
        try {
            cfg.load();
        } catch (ConfigInvalidException | IOException e) {
            logger.error("Error loading config file after get request:", e);
            return null;
        }

        JarFile jarFile = new JarFile(jarPath);
        StringWriter writer = new StringWriter();
        IOUtils.copy(jarFile.getInputStream(jarFile.getEntry("scripts/prebuild-commands.sh")), writer);
        // We must escape special characters as this will be rendered into XML
        String prebuildScript = writer.toString().replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
        params.put("cleanCommands", prebuildScript);
        writer.flush();
        writer.close();
        jarFile.close();

        String sshPort = gerritConfig.getSshdAddress();
        sshPort = sshPort.substring(sshPort.lastIndexOf(':') + 1);

        String host = canonicalWebUrl.replace("https://", "").replace("http://", "");
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(':'));
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        params.put("gerritUser", cfg.getString("Settings", "Jenkins", "gerritUser"));
        params.put("host", host);
        params.put("port", sshPort);
        params.put("credentialsId", cfg.getString("Settings", "Jenkins", "credentialsId"));
        return params;
    }

}
