package org.dce.ed.logreader;

import java.util.List;

import org.dce.ed.EliteDangerousOverlay;

public class LogPrinter {

	public static void main(String[] args) throws Exception {
		EliteJournalReader reader = new EliteJournalReader(EliteDangerousOverlay.clientKey); // auto-detect folder
		List<EliteLogEvent> events = reader.readAllEvents();

		for (EliteLogEvent e : events) {
			System.out.println(EliteLogEventFormatter.format(e));
		}
	}


}
