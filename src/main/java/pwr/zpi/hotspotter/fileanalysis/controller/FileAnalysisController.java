package pwr.zpi.hotspotter.fileanalysis.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pwr.zpi.hotspotter.fileanalysis.service.FileAnalysisService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis/file")
public class FileAnalysisController {

    private final FileAnalysisService fileAnalysisService;

}
