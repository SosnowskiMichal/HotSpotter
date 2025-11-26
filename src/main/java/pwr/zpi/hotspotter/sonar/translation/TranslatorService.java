package pwr.zpi.hotspotter.sonar.translation;

import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslateTextRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.config.GoogleProperties;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TranslatorService {

    private final GoogleProperties googleProperties;

    public String translate(String text, String sourceLang, String targetLang) throws IOException {

        try (TranslationServiceClient client = TranslationServiceClient.create()) {

            LocationName parent = LocationName.of(googleProperties.getProjectId(), "global");

            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .addContents(text)
                    .setSourceLanguageCode(sourceLang)
                    .setTargetLanguageCode(targetLang)
                    .build();

            var response = client.translateText(request);

            return response.getTranslationsList()
                    .stream()
                    .findFirst()
                    .map(Translation::getTranslatedText)
                    .orElse(null);
        }
    }
}
