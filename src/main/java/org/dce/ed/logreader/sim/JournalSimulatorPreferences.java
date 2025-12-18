package org.dce.ed.logreader.sim;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JournalSimulatorPreferences {

	public static URI getSimulatorOutputDir() {
		Path path = Paths.get("./logs");
		URI uri = path.toUri();
		return uri;
	}

	public static double getSimulatorIntervalSeconds() {
		return 1.0;
	}

}
