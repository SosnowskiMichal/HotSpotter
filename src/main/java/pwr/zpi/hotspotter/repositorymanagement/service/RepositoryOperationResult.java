package pwr.zpi.hotspotter.repositorymanagement.service;

import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

public record RepositoryOperationResult(boolean success, String message, RepositoryInfo repositoryInfo) {

    public static RepositoryOperationResult success(String message, RepositoryInfo repositoryInfo) {
        return new RepositoryOperationResult(true, message, repositoryInfo);
    }

    public static RepositoryOperationResult failure(String message) {
        return new RepositoryOperationResult(false, message, null);
    }

    public String getLocalPath() {
        return repositoryInfo != null ? repositoryInfo.getLocalPath() : null;
    }

}
