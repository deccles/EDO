package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class FileheaderEvent extends EliteLogEvent {
        private final int part;
        private final String language;
        private final boolean odyssey;
        private final String gameVersion;
        private final String build;

        public FileheaderEvent(Instant timestamp,
                               JsonObject rawJson,
                               int part,
                               String language,
                               boolean odyssey,
                               String gameVersion,
                               String build) {
            super(timestamp, EliteEventType.FILEHEADER, rawJson);
            this.part = part;
            this.language = language;
            this.odyssey = odyssey;
            this.gameVersion = gameVersion;
            this.build = build;
        }

        public int getPart() {
            return part;
        }

        public String getLanguage() {
            return language;
        }

        public boolean isOdyssey() {
            return odyssey;
        }

        public String getGameVersion() {
            return gameVersion;
        }

        public String getBuild() {
            return build;
        }
    }