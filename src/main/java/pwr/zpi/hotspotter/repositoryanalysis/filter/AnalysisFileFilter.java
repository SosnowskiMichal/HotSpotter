package pwr.zpi.hotspotter.repositoryanalysis.filter;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AnalysisFileFilter {

    private final List<Pattern> exclusionPatterns;

    public AnalysisFileFilter() {
        this.exclusionPatterns = initializeExclusionPatterns();
    }

    private List<Pattern> initializeExclusionPatterns() {
        List<String> patterns = new ArrayList<>();

        // Documentation files
        patterns.add(".*\\.(md|txt|rst|adoc|asciidoc|tex|textile)$");
        patterns.add(".*/?(README|LICENSE|CHANGELOG|CONTRIBUTING|AUTHORS|NOTICE|COPYING)(\\..+)?$");

        // Data and configuration files
        patterns.add(".*\\.(json|csv|tsv|xml|yaml|yml|toml|ini|conf|config|properties|env)$");
        patterns.add(".*package-lock\\.json$");
        patterns.add(".*yarn\\.lock$");
        patterns.add(".*pnpm-lock\\.yaml$");
        patterns.add(".*Gemfile\\.lock$");
        patterns.add(".*composer\\.lock$");
        patterns.add(".*poetry\\.lock$");
        patterns.add(".*Pipfile\\.lock$");
        patterns.add(".*Cargo\\.lock$");
        patterns.add(".*go\\.sum$");

        // Media and asset files
        patterns.add(".*\\.(png|jpg|jpeg|gif|bmp|svg|ico|webp|tiff|tif)$");
        patterns.add(".*\\.(mp4|avi|mov|wmv|flv|mkv|webm|m4v)$");
        patterns.add(".*\\.(mp3|wav|ogg|flac|aac|m4a|wma)$");
        patterns.add(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|odt|ods|odp)$");
        patterns.add(".*\\.(ttf|otf|woff|woff2|eot)$");

        // Build artifacts and compiled files
        patterns.add(".*\\.(class|jar|war|ear|nar)$");
        patterns.add(".*\\.(exe|dll|so|dylib|o|a|lib|obj)$");
        patterns.add(".*\\.(pyc|pyo|pyd)$");
        patterns.add(".*\\.(beam|boot)$");
        patterns.add(".*\\.min\\.(js|css)$");
        patterns.add(".*\\.(map)$"); // Source maps

        // Archive files
        patterns.add(".*\\.(zip|tar|gz|bz2|xz|7z|rar|tgz|tbz2)$");

        // Database files
        patterns.add(".*\\.(db|sqlite|sqlite3|mdb)$");

        // Log files
        patterns.add(".*\\.(log)$");

        // Backup and temporary files
        patterns.add(".*\\.(bak|backup|tmp|temp|swp|swo|cache)$");
        patterns.add(".*~$");

        // IDE and editor files
        patterns.add(".*\\.(iml)$");
        patterns.add("(.*/)?\\.idea/.*");
        patterns.add("(.*/)?\\.vscode/.*");
        patterns.add("(.*/)?\\.vs/.*");

        // Localization/translation files
        patterns.add(".*\\.(po|pot|mo)$");
        patterns.add(".*\\.(xliff|xlf)$");
        patterns.add(".*\\.strings$");
        patterns.add(".*\\.resx$");
        patterns.add(".*\\.arb$");

        // Version control
        patterns.add("(.*/)?\\.git/.*");
        patterns.add("(.*/)?\\.svn/.*");
        patterns.add("(.*/)?\\.hg/.*");

        // Language pack and localization directories
        patterns.add("(.*/)?i18n/.*");
        patterns.add("(.*/)?l10n/.*");
        patterns.add("(.*/)?locale/.*");
        patterns.add("(.*/)?locales/.*");
        patterns.add("(.*/)?translations?/.*");
        patterns.add("(.*/)?lang/.*");
        patterns.add("(.*/)?languages/.*");

        // Build and dependency directories
        patterns.add("(.*/)?node_modules/.*");
        patterns.add("(.*/)?bower_components/.*");
        patterns.add("(.*/)?vendor/.*");
        patterns.add("(.*/)?target/.*");
        patterns.add("(.*/)?build/.*");
        patterns.add("(.*/)?dist/.*");
        patterns.add("(.*/)?out/.*");
        patterns.add("(.*/)?__pycache__/.*");
        patterns.add("(.*/)?\\.gradle/.*");
        patterns.add("(.*/)?\\.mvn/.*");

        // Dotfiles without extensions (configuration files like .gitignore, .dockerignore, .prettierrc, etc.)
        patterns.add("(.*/)?\\.(?!.*/)[^/.]+$");

        // Files without extensions (extensionless files)
        // Exclude common extensionless files that might be important (like Dockerfile, Makefile)
        patterns.add("^(?!.*(Dockerfile|Makefile|Rakefile|Vagrantfile|Jenkinsfile|Procfile|Gemfile|Guardfile|Brewfile)).*[^/]+/[^/.]+$");
        patterns.add("^(?!.*(Dockerfile|Makefile|Rakefile|Vagrantfile|Jenkinsfile|Procfile|Gemfile|Guardfile|Brewfile))[^/.]+$");

        return patterns.stream()
                .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    public boolean shouldIncludeFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        String normalizedPath = filePath.replace('\\', '/');

        for (Pattern pattern : exclusionPatterns) {
            if (pattern.matcher(normalizedPath).matches()) {
                return false;
            }
        }

        return true;
    }

    public Commit filterCommit(Commit commit) {
        if (commit == null || commit.changedFiles() == null) {
            return commit;
        }

        List<FileChange> filteredFiles = commit.changedFiles().stream()
                .filter(fileChange -> shouldIncludeFile(fileChange.filePath()))
                .toList();

        if (filteredFiles.size() == commit.changedFiles().size()) {
            return commit;
        }

        return new Commit(
                commit.hash(),
                commit.date(),
                commit.author(),
                commit.email(),
                filteredFiles
        );
    }

    public Set<String> filterFileNames(Set<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return fileNames;
        }

        return fileNames.stream()
                .filter(this::shouldIncludeFile)
                .collect(Collectors.toSet());
    }

}
