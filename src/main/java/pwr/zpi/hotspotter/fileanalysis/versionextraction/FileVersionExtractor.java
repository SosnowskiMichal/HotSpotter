package pwr.zpi.hotspotter.fileanalysis.versionextraction;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.AnalysisException;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class FileVersionExtractor {

    public void extractFileVersions(Path repositoryPath, String filePath, List<String> commitHashes, Path outputPath) {
        if (repositoryPath == null || filePath == null || commitHashes == null || outputPath == null) return;

        String fileExtension = FilenameUtils.getExtension(filePath);

        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (Git git = Git.open(repositoryPath.toFile()); Repository repository = git.getRepository()) {
                List<RevCommit> commits = getAllCommitsForFile(git, filePath);
                Map<String, String> commitToPathMap = buildCommitToPathMap(repository, filePath, commits);

                try (RevWalk revWalk = new RevWalk(repository)) {
                    for (String commitHash : commitHashes) {
                        ObjectId commitId = repository.resolve(commitHash);
                        if (commitId == null) continue;

                        String fullHash = commitId.getName();
                        RevCommit commit = revWalk.parseCommit(commitId);
                        RevTree tree = commit.getTree();

                        String filePathInCommit = commitToPathMap.get(fullHash);
                        if (filePathInCommit == null) {
                            filePathInCommit = filePath.replace("\\", "/");
                        }

                        try (TreeWalk treeWalk = new TreeWalk(repository)) {
                            treeWalk.addTree(tree);
                            treeWalk.setRecursive(true);
                            treeWalk.setFilter(PathFilter.create(filePathInCommit));

                            if (treeWalk.next()) {
                                ObjectId objectId = treeWalk.getObjectId(0);
                                ObjectLoader loader = repository.open(objectId);

                                Path outputFilePath = outputPath.resolve(commitHash + "." + fileExtension);
                                try (OutputStream outputStream = Files.newOutputStream(outputFilePath)) {
                                    loader.copyTo(outputStream);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new AnalysisException("Failed to extract file versions: " + e.getMessage());
        }
    }

    private List<RevCommit> getAllCommitsForFile(Git git, String filePath) throws Exception {
        List<RevCommit> commits = new ArrayList<>();

        Iterable<RevCommit> logs = git.log().addPath(filePath).call();
        for (RevCommit commit : logs) {
            commits.add(commit);
        }

        Collections.reverse(commits);
        return commits;
    }

    private Map<String, String> buildCommitToPathMap(
            Repository repository,
            String filePath,
            List<RevCommit> commits
    ) throws Exception {
        Map<String, String> commitToPath = new HashMap<>();
        String currentPath = filePath;

        try (ObjectReader reader = repository.newObjectReader()) {
            for (int i = commits.size() - 1; i >= 0; i--) {
                RevCommit commit = commits.get(i);
                commitToPath.put(commit.getName(), currentPath);

                if (i > 0) {
                    RevCommit parentCommit = commits.get(i - 1);

                    CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                    oldTreeParser.reset(reader, parentCommit.getTree());

                    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                    newTreeParser.reset(reader, commit.getTree());

                    List<DiffEntry> diffs = new Git(repository).diff()
                            .setOldTree(oldTreeParser)
                            .setNewTree(newTreeParser)
                            .call();

                    RenameDetector renameDetector = new RenameDetector(repository);
                    renameDetector.addAll(diffs);
                    List<DiffEntry> renames = renameDetector.compute();

                    for (DiffEntry diff : renames) {
                        if (diff.getChangeType() == DiffEntry.ChangeType.RENAME && diff.getNewPath().equals(currentPath)) {
                            currentPath = diff.getOldPath();
                            break;
                        }
                    }
                }
            }
        }

        return commitToPath;
    }

}
