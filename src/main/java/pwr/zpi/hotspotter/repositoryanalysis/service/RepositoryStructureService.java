package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure.RepositoryStructureNode;
import pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure.RepositoryStructureResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RepositoryStructureService {

    public RepositoryStructureResponse buildRepositoryStructure(
            Collection<FileInfo> fileInfoData,
            Collection<FileKnowledge> fileKnowledgeData) {

        Map<String, FileInfo> fileInfoMap = fileInfoData.stream()
                .collect(Collectors.toMap(FileInfo::getFilePath, fi -> fi));
        Map<String, FileKnowledge> fileKnowledgeMap = fileKnowledgeData.stream()
                .collect(Collectors.toMap(FileKnowledge::getFilePath, fi -> fi));

        RepositoryStructureNode root = buildTree(fileInfoMap, fileKnowledgeMap);

        MaxFileValues maxFileValues = calculateMaxFileValues(fileInfoData);
        setFileDimensions(root, maxFileValues);
        calculateDirectoryStatistics(root);

        RepositoryStructureResponse.ReferenceData referenceData = RepositoryStructureResponse.ReferenceData.builder()
                .maxCommits(maxFileValues.maxCommits)
                .maxCommitsInHotSpotAnalysisPeriod(maxFileValues.maxCommitsInHotSpotAnalysisPeriod)
                .maxLinesOfCode(maxFileValues.maxLinesOfCode)
                .build();

        return RepositoryStructureResponse.builder()
                .structure(root)
                .referenceData(referenceData)
                .build();
    }

    private RepositoryStructureNode buildTree(
            Map<String, FileInfo> fileInfoMap,
            Map<String, FileKnowledge> fileKnowledgeMap) {

        RepositoryStructureNode root = RepositoryStructureNode.builder()
                .name("root")
                .type("dir")
                .path("")
                .build();

        Map<String, RepositoryStructureNode> directoryNodesMap = new HashMap<>();
        directoryNodesMap.put("", root);

        fileInfoMap.forEach((filePath, fileInfo) -> {
            FileKnowledge fileKnowledge = fileKnowledgeMap.get(filePath);
            addFileToTree(filePath, fileInfo, fileKnowledge, root, directoryNodesMap);
        });

        return root;
    }

    private void addFileToTree(
            String filePath,
            FileInfo fileInfo,
            FileKnowledge fileKnowledge,
            RepositoryStructureNode root,
            Map<String, RepositoryStructureNode> directoryNodesMap) {

        String[] pathSegments = filePath.split("/");
        RepositoryStructureNode parentNode = ensureDirectoryPath(pathSegments, root, directoryNodesMap);

        String fileName = fileInfo.getFileName();
        RepositoryStructureNode fileNode = createFileNode(fileName, filePath, fileInfo, fileKnowledge);
        parentNode.addChild(fileNode);
    }

    private RepositoryStructureNode ensureDirectoryPath(
            String[] pathSegments,
            RepositoryStructureNode root,
            Map<String, RepositoryStructureNode> directoryNodesMap) {

        StringBuilder currentPath = new StringBuilder();
        RepositoryStructureNode parentNode = root;

        for (int i = 0; i < pathSegments.length - 1; i++) {
            String segment = pathSegments[i];

            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(segment);
            String currentPathStr = currentPath.toString();

            RepositoryStructureNode directoryNode = directoryNodesMap.get(currentPathStr);
            if (directoryNode == null) {
                directoryNode = createDirectoryNode(segment, currentPathStr);
                directoryNodesMap.put(currentPathStr, directoryNode);
                parentNode.addChild(directoryNode);
            }

            parentNode = directoryNode;
        }

        return parentNode;
    }

    private RepositoryStructureNode createDirectoryNode(String directoryName, String directoryPath) {
        return RepositoryStructureNode.builder()
                .name(directoryName)
                .path(directoryPath)
                .type("dir")
                .build();
    }

    private RepositoryStructureNode createFileNode(
            String fileName,
            String filePath,
            FileInfo fileInfo,
            FileKnowledge fileKnowledge) {

        return RepositoryStructureNode.builder()
                .name(fileName)
                .path(filePath)
                .type("file")
                .fileType(fileInfo.getFileType())
                .fileSize(fileInfo.getFileSize())
                .linesOfCode(fileInfo.getCodeLines())
                .commits(fileInfo.getTotalCommits())
                .commitsInHotSpotAnalysisPeriod(fileInfo.getCommitsInHotSpotAnalysisPeriod())
                .commitsLastYear(fileInfo.getCommitsLastYear())
                .firstCommitDate(fileInfo.getFirstCommitDate())
                .lastCommitDate(fileInfo.getLastCommitDate())
                .leadAuthor(fileKnowledge.getLeadAuthor())
                .leadAuthorKnowledgePercentage(fileKnowledge.getLeadAuthorKnowledgePercentage())
                .contributors(fileKnowledge.getContributors())
                .activeContributors(fileKnowledge.getActiveContributors())
                .build();
    }

    private MaxFileValues calculateMaxFileValues(Collection<FileInfo> fileInfoData) {
        int maxCommits = fileInfoData.stream()
                .mapToInt(fi -> fi.getTotalCommits() != null ? fi.getTotalCommits() : 0)
                .max()
                .orElse(0);
        int maxCommitsInHotSpotAnalysisPeriod = fileInfoData.stream()
                .mapToInt(fi -> fi.getCommitsInHotSpotAnalysisPeriod() != null ? fi.getCommitsInHotSpotAnalysisPeriod() : 0)
                .max()
                .orElse(0);
        int maxLinesOfCode = fileInfoData.stream()
                .mapToInt(fi -> fi.getCodeLines() != null ? fi.getCodeLines() : 0)
                .max()
                .orElse(0);

        return new MaxFileValues(maxCommits, maxCommitsInHotSpotAnalysisPeriod, maxLinesOfCode);
    }

    private void setFileDimensions(RepositoryStructureNode node, MaxFileValues maxFileValues) {
        if (node.getType().equals("dir")) {
            if (node.getChildren() != null) {
                node.getChildren().forEach(child -> setFileDimensions(child, maxFileValues));
            }
            return;
        }

        if (node.getCommits() != null && maxFileValues.maxCommits > 0) {
            double normalizedValue = (double) node.getCommits() / maxFileValues.maxCommits;
            double height = Math.round((Math.exp(2 * normalizedValue) - 1) / (Math.exp(2) - 1) * 100.0) / 100.0;
            node.setHeight(height);
        } else {
            node.setHeight(0.0);
        }

        if (node.getLinesOfCode() != null && maxFileValues.maxLinesOfCode > 0) {
            double width = Math.round(node.getLinesOfCode() * 100.0 / maxFileValues.maxLinesOfCode) / 100.0;
            node.setWidth(width);
        } else {
            node.setWidth(0.0);
        }
    }

    private NodeStats calculateDirectoryStatistics(RepositoryStructureNode node) {
        if (node.getType().equals("file")) {
            return new NodeStats(
                    1,
                    node.getLinesOfCode() != null ? node.getLinesOfCode() : 0,
                    node.getCommits() != null ? node.getCommits() : 0
            );
        }

        NodeStats aggregatedStats = new NodeStats(0, 0, 0);

        if (node.getChildren() != null) {
            node.getChildren().forEach(child -> {
                NodeStats childStats = calculateDirectoryStatistics(child);
                aggregatedStats.numberOfFiles += childStats.numberOfFiles;
                aggregatedStats.totalLinesOfCode += childStats.totalLinesOfCode;
                aggregatedStats.totalCommits += childStats.totalCommits;
            });

            node.setNumberOfFiles(aggregatedStats.numberOfFiles);
            node.setLinesOfCode(aggregatedStats.totalLinesOfCode);
            node.setAverageCommits(aggregatedStats.totalCommits / aggregatedStats.numberOfFiles);
        }

        return aggregatedStats;
    }

    private record MaxFileValues(int maxCommits, int maxCommitsInHotSpotAnalysisPeriod, int maxLinesOfCode) { }

    @AllArgsConstructor
    private static class NodeStats {
        public int numberOfFiles;
        public int totalLinesOfCode;
        public int totalCommits;
    }

}
