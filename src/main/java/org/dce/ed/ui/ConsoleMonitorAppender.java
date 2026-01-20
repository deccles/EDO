/*
This is the copyright work of The MITRE Corporation, and was produced for the U. S. Government under
Contract Number 693KA8-22-C-00001, and is subject to Federal Aviation Administration Acquisition Management System
Clause 3.5-13, Rights In Data-General (Oct. 2014), Alt. III and Alt. IV (Jan. 2009).  No other use other than that
granted to the U. S. Government, or to those acting on behalf of the U. S. Government, under that Clause is authorized
without the express written permission of The MITRE Corporation. For further information, please contact The MITRE
Corporation, Contracts Management Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.

© 2025 The MITRE Corporation. All Rights Reserved.
 */
package org.dce.ed.ui;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "ConsoleMonitorAppender", category = "Core", elementType = "appender", printObject = true)
public class ConsoleMonitorAppender extends AbstractAppender {

    private static ConsoleMonitor consoleMonitor;

    protected ConsoleMonitorAppender(String name, Layout<?> layout) {
        super(name, null, layout, false);
    }

    @Override
    public void append(LogEvent event) {
        if (consoleMonitor != null) {
            String message = new String(getLayout().toByteArray(event));
            consoleMonitor.appendLine(message);
        }
    }

    public static void setConsoleMonitor(ConsoleMonitor monitor) {
        consoleMonitor = monitor;
    }

    @PluginFactory
    public static ConsoleMonitorAppender createAppender(@PluginAttribute("name") String name,
                                                        @PluginElement("Layout") Layout<?> layout) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new ConsoleMonitorAppender(name, layout);
    }
}
