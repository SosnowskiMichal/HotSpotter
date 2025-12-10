package pwr.zpi.hotspotter.fileanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exception.ObjectNotFoundException;
import pwr.zpi.hotspotter.fileanalysis.mapper.FileAnalysisResultMapper;
import pwr.zpi.hotspotter.fileanalysis.model.FileAnalysisResult;
import pwr.zpi.hotspotter.fileanalysis.repository.FileAnalysisResultRepository;
import pwr.zpi.hotspotter.fileanalysis.dto.FileAnalysisResultDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisResultsService {

    private final FileAnalysisResultRepository fileAnalysisResultRepository;

    private final FileAnalysisResultMapper fileAnalysisResultMapper;

    public boolean checkIfFileAnalysisResultExists(String analysisId, String filePath) {
        return fileAnalysisResultRepository.existsByAnalysisIdAndFilePath(analysisId, filePath);
    }

    public boolean checkIfFileAnalysisCompleted(String analysisId, String filePath) {
        return fileAnalysisResultRepository.isAnalysisCompleted(analysisId, filePath);
    }

    public FileAnalysisResultDTO getFileAnalysisResult(String analysisId, String filePath) {
        FileAnalysisResult fileAnalysisResult = fileAnalysisResultRepository
                .findByAnalysisIdAndFilePath(analysisId, filePath)
                .orElseThrow(() -> {
                    log.warn("File analysis result not found for analysis ID: {} and file path: {}", analysisId, filePath);
                    return new ObjectNotFoundException("File analysis result not found.");
                });

        return fileAnalysisResultMapper.toDTO(fileAnalysisResult);
    }

}
