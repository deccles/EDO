package org.dce.ed.logreader;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tails the latest Elite Dangerous journal file and emits EliteLogEvent
 * objects to registered listeners in (roughly) real time.
 *
 * This class does NOT replay history on startup; the UI that wants
 * history (e.g. SystemTabPanel) should use EliteJournalReader once at
 * construction time, then rely on this monitor for new events only.
 */
public final class LiveJournalMonitor {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private static final LiveJournalMonitor INSTANCE = new LiveJournalMonitor();

    private final CopyOnWriteArrayList<Consumer<EliteLogEvent>> listeners =
            new CopyOnWriteArrayList<>();

    private final EliteLogParser parser = new EliteLogParser();

    private volatile boolean running = false;
    private Thread workerThread;

    private LiveJournalMonitor() {
    }

    public static LiveJournalMonitor getInstance() {
        return INSTANCE;
    }

    public void addListener(Consumer<EliteLogEvent> listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        startIfNeeded();
    }

    public void removeListener(Consumer<EliteLogEvent> listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
    }

    public void shutdown() {
        running = false;
        Thread t = workerThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private synchronized void startIfNeeded() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this::runLoop, "Elite-LiveJournalMonitor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void runLoop() {
        Path journalDir = EliteLogFileLocator.findDefaultJournalDirectory();
        if (journalDir == null || !Files.isDirectory(journalDir)) {
            running = false;
            return;
        }

        Path currentFile = null;
        long filePointer = 0L;

        while (running) {
            try {
                Path latest = findLatestJournalFile(journalDir);

                if (!Objects.equals(latest, currentFile)) {
                    // new or rotated file: start tailing from the end
                    currentFile = latest;
                    filePointer = (currentFile != null && Files.isRegularFile(currentFile))
                            ? Files.size(currentFile)
                            : 0L;
                }

                if (currentFile != null) {
                    filePointer = readFromFile(currentFile, filePointer);
                }

                try {
                    Thread.sleep(POLL_INTERVAL.toMillis());
                } catch (InterruptedException ie) {
                    if (!running) {
                        break;
                    }
                }
            } catch (Exception ex) {
                try {
                    Thread.sleep(POLL_INTERVAL.toMillis());
                } catch (InterruptedException ignored) {
                    // ignore
                }
            }
        }
    }

    private long readFromFile(Path file, long startPos) {
        if (!Files.isRegularFile(file)) {
            return startPos;
        }

        long newPos = startPos;

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(startPos);

            String raw;
            while ((raw = raf.readLine()) != null) {
                newPos = raf.getFilePointer();

                // Convert from ISO-8859-1 assumption to UTF-8
                String line = new String(raw.getBytes(StandardCharsets.ISO_8859_1),
                                         StandardCharsets.UTF_8).trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    EliteLogEvent event = parser.parseRecord(line);
                    if (event != null) {
                        dispatch(event);
                    }
                } catch (JsonSyntaxException | IllegalStateException ex) {
                    // malformed line – skip
                }
            }
        } catch (IOException ex) {
            // transient I/O – skip; retry next poll
        }

        return newPos;
    }

    private void dispatch(EliteLogEvent event) {
        for (Consumer<EliteLogEvent> l : listeners) {
            try {
                l.accept(event);
            } catch (RuntimeException ex) {
                // don't let one bad listener break the others
            }
        }
    }

    private Path findLatestJournalFile(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> candidates = s
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("Journal.") && name.endsWith(".log");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                return null;
            }
            return candidates.get(candidates.size() - 1);
        }
    }
}
