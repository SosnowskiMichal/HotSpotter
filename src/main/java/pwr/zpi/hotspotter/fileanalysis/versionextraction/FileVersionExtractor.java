package pwr.zpi.hotspotter.fileanalysis.versionextraction;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class FileVersionExtractor {

    public void extractFileVersions(Path repositoryPath, List<FileCommit> fileCommits, Path outputPath) {
        if (repositoryPath == null || fileCommits == null || fileCommits.isEmpty() || outputPath == null) return;

        String fileExtension = FilenameUtils.getExtension(fileCommits.getFirst().path());
        if (fileExtension == null || fileExtension.isEmpty()) {
            throw new AnalysisException("Could not determine file extension from file path: " + fileCommits.getFirst().path());
        }

        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (Git git = Git.open(repositoryPath.toFile()); Repository repository = git.getRepository()) {
                try (RevWalk revWalk = new RevWalk(repository)) {
                    for (FileCommit fileCommit : fileCommits) {
                        ObjectId commitId = repository.resolve(fileCommit.hash());
                        if (commitId == null) continue;

                        RevCommit commit = revWalk.parseCommit(commitId);
                        RevTree tree = commit.getTree();

                        String filePathInCommit = fileCommit.path();
                        if (filePathInCommit == null) continue;

                        try (TreeWalk treeWalk = new TreeWalk(repository)) {
                            treeWalk.addTree(tree);
                            treeWalk.setRecursive(true);
                            treeWalk.setFilter(PathFilter.create(filePathInCommit));

                            if (treeWalk.next()) {
                                ObjectId objectId = treeWalk.getObjectId(0);
                                ObjectLoader loader = repository.open(objectId);

                                Path outputFilePath = outputPath.resolve(fileCommit.hash() + "." + fileExtension);
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

}
