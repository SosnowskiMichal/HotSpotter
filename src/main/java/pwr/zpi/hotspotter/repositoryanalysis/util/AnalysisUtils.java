package pwr.zpi.hotspotter.repositoryanalysis.util;

import lombok.experimental.UtilityClass;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class AnalysisUtils {

    public static final int DEFAULT_SAVE_BATCH_SIZE = 500;

    public static <T, ID> void saveDataInBatches(CrudRepository<T, ID> repository, Iterable<T> data, int batchSize)
            throws IllegalArgumentException{

        if (batchSize < 1) throw new IllegalArgumentException("Batch size must be at least 1");
        if (repository == null || data == null) return;

        List<T> dataList = toList(data);
        if (dataList.isEmpty()) return;

        int totalSize = dataList.size();
        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<T> batch = dataList.subList(i, end);
            repository.saveAll(batch);
        }
    }

    public static <T, ID> void saveDataInBatches(CrudRepository<T, ID> repository, Iterable<T> data)
            throws IllegalArgumentException {

        saveDataInBatches(repository, data, DEFAULT_SAVE_BATCH_SIZE);
    }

    private static <T> List<T> toList(Iterable<T> data) {
        if (data instanceof List) return (List<T>) data;
        if (data instanceof Collection) return new ArrayList<>((Collection<T>) data);

        List<T> list = new ArrayList<>();
        data.forEach(list::add);
        return list;
    }

}
