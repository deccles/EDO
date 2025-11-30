package org.dce.ed.logreader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class LogPrinter {

	public static void main(String[] args) throws Exception {
		EliteJournalReader reader = new EliteJournalReader(); // auto-detect folder
		List<EliteLogEvent> events = reader.readAllEvents();

		for (EliteLogEvent e : events) {
			System.out.println(EliteLogEventFormatter.format(e));
		}
	}


}
