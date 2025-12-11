package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisHistoryEntryDTO;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.AnalysisInfoMapper;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.user.model.User;

@Service
@RequiredArgsConstructor
public class RepositoryAnalysisHistoryService {

    private final AnalysisInfoRepository analysisInfoRepository;
    private final AnalysisInfoMapper analysisInfoMapper;

    public Page<AnalysisHistoryEntryDTO> getUserAnalysisHistory(User user, Pageable pageable) {
        Page<AnalysisInfo> analysisPage = analysisInfoRepository.findByUserId(user.getId(), pageable);
        return analysisPage.map(analysisInfoMapper::toHistoryDTO);
    }

}
