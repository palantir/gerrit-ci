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
package com.palantir.gerrit.gerritci.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ConfigServlet extends HttpServlet {

    private static final long serialVersionUID = 3261707607741902411L;
	private static final Logger logger = LoggerFactory.getLogger(ConfigServlet.class);
	private final SitePaths sitePaths;
	private final Provider<CurrentUser> currentUser;

	@Inject
	public ConfigServlet(SitePaths sitePaths,
			final ProjectControl.Factory projectControlFactory,
			Provider<CurrentUser> currentUser) {
		this.sitePaths = sitePaths;
		this.currentUser = currentUser;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		if (currentUser.get().getCapabilities().canAdministrateServer() == false) {
			res.setStatus(403);
			return;
		}

		FileBasedConfig cfg = new FileBasedConfig(new File(sitePaths.etc_dir,
				"gerrit-ci.config"), FS.DETECTED);
		try {
			cfg.load();
		} catch (ConfigInvalidException e) {
			logger.error("Received GET Request. Error loading gerrit-ci.config file:", e );
		}

		JsonObject params = new JsonObject();
		params.addProperty("jenkinsURL",
				cfg.getString("Settings", "Jenkins", "jenkinsURL"));
		params.addProperty("jenkinsUser",
				cfg.getString("Settings", "Jenkins", "jenkinsUser"));
		params.addProperty("jenkinsPassword",
				cfg.getString("Settings", "Jenkins", "jenkinsPassword"));
		params.addProperty("gerritUser",
				cfg.getString("Settings", "Jenkins", "gerritUser"));
		params.addProperty("credentialsId",
				cfg.getString("Settings", "Jenkins", "credentialsId"));

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		res.getWriter().write(params.toString());
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		if (currentUser.get().getCapabilities().canAdministrateServer() == false) {
			res.setStatus(403);
			return;
		}

		/*
		 * The actual parameters we send are encoded into a JSON object such
		 * that they are contained in an object under the entry "f". The other
		 * top-level keys seem to be useless. In addition, each key in the
		 * parameters object has ":" prefixed to whatever it is the key is
		 * actually named. Thus, by stripping the first character away from each
		 * key, we arrive at a sane JSONObject of request parameters. Example:
		 * {"b": [], "f": {":projectName": "name", ":verifyBranchRegex": *
		 * ".*"}}
		 */
		JsonObject requestBody = (JsonObject) (new JsonParser())
				.parse(CharStreams.toString(req.getReader()));
		requestBody = requestBody.get("f").getAsJsonObject();
		File confFile = new File(sitePaths.etc_dir, "gerrit-ci.config");
		FileBasedConfig cfg = new FileBasedConfig(confFile, FS.DETECTED);
		try {
			cfg.load();
		} catch (ConfigInvalidException e) {
			logger.error("Received PUT request. Error loading gerrit-ci.config file:", e);
		}
		cfg.clear();

		for (Entry<String, JsonElement> entry : requestBody.entrySet()) {
			// substring(1) removes the ':' character that precedes each key in the requestBody
			cfg.setString("Settings", "Jenkins", entry.getKey().substring(1),
					entry.getValue().getAsString());
		}
		cfg.save();
		// the gerrit-ci.config file starts off with permissions -rw-rw-r--
		cfg.getFile().setReadOnly();
		// make unwritable by making file read only for everyone -r--r--r--
		cfg.getFile().setReadable(false, false);
		// Make the file unreadable (readable=false and ownerOnly=false)
		cfg.getFile().setReadable(true, true);
		// Sets the file as readable for only the owner (readable=true and ownerOnly=true) -r--------
		res.setStatus(200);
		res.setContentType("text/plain");
	}
}
