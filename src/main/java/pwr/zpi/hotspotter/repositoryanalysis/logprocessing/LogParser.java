package pwr.zpi.hotspotter.repositoryanalysis.logprocessing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class LogParser {

    private static final Pattern LOG_HEADER_PATTERN = Pattern.compile(
            "\\[(?<hash>[^]]+)]\\s" +
            "(?<date>\\d{4}-\\d{2}-\\d{2})\\n" +
            "(?<author>[^<]+)\\s<(?<email>[^>]+)>"
    );
    private static final Pattern FILE_CHANGE_PATTERN = Pattern.compile(
            "(?<added>\\d+|-)\\s+(?<removed>\\d+|-)\\s+(?<file>[^\\n]+)"
    );
    private static final Pattern FULL_RENAME_PATTERN = Pattern.compile(
            "^(?<old>[^{}]*?)\\s=>\\s(?<current>[^{}]*?)$"
    );
    private static final Pattern PARTIAL_RENAME_PATTERN = Pattern.compile(
            "\\{(?<old>[^{}]*?)\\s=>\\s(?<current>[^{}]*?)}"
    );

    public LogParsingResult parseLogs(Path logFilePath) {
        try {
            Stream<Commit> commitStream = getCommitStream(logFilePath);
            return LogParsingResult.success("Commit stream initialized.", commitStream);

        } catch (IOException e) {
            log.error("Failed to initialize commit stream for log file ({}): {}", logFilePath.toAbsolutePath(), e.getMessage());
            return LogParsingResult.failure("Failed to initialize commit stream.");
        }
    }

    private Stream<Commit> getCommitStream(Path logFilePath) throws IOException {
        CommitIterator iterator = new CommitIterator(logFilePath);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).onClose(iterator::close);
    }

    public record LogParsingResult(boolean success, String message, Stream<Commit> commits) {
        public static LogParsingResult success(String message, Stream<Commit> commits) {
            return new LogParsingResult(true, message, commits);
        }

        public static LogParsingResult failure(String message) {
            return new LogParsingResult(false, message, null);
        }
    }

    // ====================================================================================================
    // Commit Stream Iterator
    // ====================================================================================================

    private class CommitIterator implements Iterator<Commit>, AutoCloseable {
        private final Path logFilePath;
        private final BufferedReader reader;
        private final StringBuilder block;
        private Commit nextCommit;
        private boolean hasNextCached = false;
        private boolean closed = false;

        public CommitIterator(Path logFilePath) throws IOException {
            this.logFilePath = logFilePath;
            this.reader = new BufferedReader(new FileReader(logFilePath.toFile()));
            this.block = new StringBuilder(512);
            cacheNextCommit();
        }

        @Override
        public boolean hasNext() {
            return hasNextCached;
        }

        @Override
        public Commit next() {
            if (!hasNextCached) {
                throw new NoSuchElementException("No more commits available");
            }
            Commit currentCommit = nextCommit;
            cacheNextCommit();
            return currentCommit;
        }

        private void cacheNextCommit() {
            if (closed) {
                hasNextCached = false;
                return;
            }

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;

                    if (line.startsWith("[")) {
                        if (!block.isEmpty()) {
                            nextCommit = parseBlock(block.toString());
                            block.setLength(0);

                            if (nextCommit != null) {
                                block.append(line).append("\n");
                                hasNextCached = true;
                                return;
                            }
                        }
                        block.append(line).append("\n");
                    } else {
                        block.append(line).append("\n");
                    }
                }

                if (!block.isEmpty()) {
                    nextCommit = parseBlock(block.toString());
                    block.setLength(0);
                    if (nextCommit != null) {
                        hasNextCached = true;
                        return;
                    }
                }

                hasNextCached = false;
                nextCommit = null;
                close();

            } catch (IOException e) {
                log.error("Error reading log file ({}): {}", logFilePath.toAbsolutePath(), e.getMessage());
                hasNextCached = false;
                nextCommit = null;
                close();
            }
        }

        @Override
        public void close() {
            if (closed) return;

            closed = true;
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Error closing log file ({}): {}", logFilePath.toAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    private Commit parseBlock(String block) {
        Matcher matcher = LOG_HEADER_PATTERN.matcher(block);
        if (!matcher.find()) return null;

        String hash = matcher.group("hash");
        String date = matcher.group("date");
        String author = matcher.group("author");
        String email = matcher.group("email");

        int headerEnd = matcher.end();
        String filesBlock = block.substring(headerEnd).trim();
        List<FileChange> changedFiles = parseFileChanges(filesBlock);

        return new Commit(hash, date, author, email, changedFiles);
    }

    private List<FileChange> parseFileChanges(String filesBlock) {
        if (filesBlock.isBlank()) return List.of();

        List<FileChange> fileChanges = new ArrayList<>();
        Matcher matcher = FILE_CHANGE_PATTERN.matcher(filesBlock);

        while (matcher.find()) {
            String filePath = matcher.group("file").trim();
            int linesAdded = parseNumberOfLines(matcher.group("added"));
            int linesDeleted = parseNumberOfLines(matcher.group("removed"));

            FileChange fileChange = checkForFilePathChange(new FileChange(filePath, linesAdded, linesDeleted));
            fileChanges.add(fileChange);
        }

        return fileChanges;
    }

    private int parseNumberOfLines(String numberStr) {
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException _) {
            return 0;
        }
    }

    private FileChange checkForFilePathChange(FileChange fileChange) {
        String filePath = fileChange.filePath();

        Matcher fullMatcher = FULL_RENAME_PATTERN.matcher(filePath);
        if (fullMatcher.matches()) {
            return fileChange.withFilePathChange(
                    normalizePath(fullMatcher.group("old")),
                    normalizePath(fullMatcher.group("current"))
            );
        }

        Matcher partialMatcher = PARTIAL_RENAME_PATTERN.matcher(filePath);
        if (!partialMatcher.find()) {
            return fileChange;
        }

        StringBuilder oldFilePath = new StringBuilder();
        StringBuilder newFilePath = new StringBuilder();

        partialMatcher.reset();
        int lastEnd = 0;

        while (partialMatcher.find()) {
            String unchangedPath = filePath.substring(lastEnd, partialMatcher.start());
            oldFilePath.append(unchangedPath);
            newFilePath.append(unchangedPath);

            String oldPart = partialMatcher.group("old");
            String newPart = partialMatcher.group("current");

            oldFilePath.append(oldPart != null ? oldPart : "");
            newFilePath.append(newPart != null ? newPart : "");

            lastEnd = partialMatcher.end();
        }

        oldFilePath.append(filePath.substring(lastEnd));
        newFilePath.append(filePath.substring(lastEnd));

        return fileChange.withFilePathChange(
                normalizePath(oldFilePath.toString()),
                normalizePath(newFilePath.toString())
        );
    }

    private String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/").trim();
    }

}
