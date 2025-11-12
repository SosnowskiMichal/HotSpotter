package pwr.zpi.hotspotter.repositoryanalysis.service;

import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.model.RepositoryStructureNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RepositoryStructureService {

    public RepositoryStructureNode buildRepositoryStructure(Collection<FileInfo> fileInfoData) {
        Map<String, FileInfo> fileInfoMap = fileInfoData.stream()
                .collect(Collectors.toMap(FileInfo::getFilePath, fi -> fi));

        RepositoryStructureNode root = buildTree(fileInfoMap);

        MaxFileValues maxFileValues = calculateMaxFileValues(fileInfoData);
        setFileDimensions(root, maxFileValues, fileInfoMap);

        return root;
    }

    private RepositoryStructureNode buildTree(Map<String, FileInfo> fileInfoMap) {
        RepositoryStructureNode root = RepositoryStructureNode.builder()
                .name("root")
                .type("dir")
                .path("")
                .build();

        Map<String, RepositoryStructureNode> directoryNodesMap = new HashMap<>();
        directoryNodesMap.put("", root);

        fileInfoMap.forEach((filePath, fileInfo) ->
            addFileToTree(filePath, fileInfo, root, directoryNodesMap)
        );

        return root;
    }

    private void addFileToTree(
            String filePath,
            FileInfo fileInfo,
            RepositoryStructureNode root,
            Map<String, RepositoryStructureNode> directoryNodesMap) {

        String[] pathSegments = filePath.split("/");
        RepositoryStructureNode parentNode = ensureDirectoryPath(pathSegments, root, directoryNodesMap);

        String fileName = fileInfo.getFileName();
        RepositoryStructureNode fileNode = createFileNode(fileName, filePath);
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

    private RepositoryStructureNode createFileNode(String fileName, String filePath) {
        return RepositoryStructureNode.builder()
                .name(fileName)
                .path(filePath)
                .type("file")
                .build();
    }

    private MaxFileValues calculateMaxFileValues(Collection<FileInfo> fileInfoData) {
        int maxCommits = fileInfoData.stream()
                .mapToInt(fi -> Objects.requireNonNullElse(fi.getTotalCommits(), 0))
                .max()
                .orElse(0);
        int maxLinesOfCode = fileInfoData.stream()
                .mapToInt(fi -> Objects.requireNonNullElse(fi.getCodeLines(), 0))
                .max()
                .orElse(0);

        return new MaxFileValues(maxCommits, maxLinesOfCode);
    }

    private void setFileDimensions(RepositoryStructureNode node, MaxFileValues maxFileValues, Map<String, FileInfo> fileInfoMap) {
        if (node.getType().equals("dir")) {
            if (node.getChildren() != null) {
                node.getChildren().forEach(child -> setFileDimensions(child, maxFileValues, fileInfoMap));
            }
            return;
        }

        FileInfo fileInfo = fileInfoMap.get(node.getPath());
        if (fileInfo == null) {
            node.setHeight(0.0);
            node.setWidth(0.0);
            return;
        }

        Integer commits = fileInfo.getTotalCommits();
        if (commits != null && maxFileValues.maxCommits > 0) {
            double normalizedValue = (double) commits / maxFileValues.maxCommits;
            double height = Math.round((Math.exp(2 * normalizedValue) - 1) / (Math.exp(2) - 1) * 100.0) / 100.0;
            node.setHeight(height);
        } else {
            node.setHeight(0.0);
        }

        Integer linesOfCode = fileInfo.getCodeLines();
        if (linesOfCode != null && maxFileValues.maxLinesOfCode > 0) {
            double width = Math.round(linesOfCode * 100.0 / maxFileValues.maxLinesOfCode) / 100.0;
            node.setWidth(width);
        } else {
            node.setWidth(0.0);
        }
    }

    private record MaxFileValues(int maxCommits, int maxLinesOfCode) { }

}
