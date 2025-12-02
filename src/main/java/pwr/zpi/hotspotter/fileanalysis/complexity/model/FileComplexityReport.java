package pwr.zpi.hotspotter.fileanalysis.complexity.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileComplexityReport {
    private int totalCCN;
    private double averageCCN;
    private int maxCCN;
    private int functionsCount;
    private List<MethodComplexity> methods = new ArrayList<>();

    public void addFunctionStats(int ccn) {
        this.totalCCN += ccn;
        this.functionsCount++;
        if (ccn > this.maxCCN) {
            this.maxCCN = ccn;
        }
        this.averageCCN = (double) totalCCN / functionsCount;
    }

    public void addFunctionStats(String name, int ccn, int params, int start, int end) {
        addFunctionStats(ccn);
        this.methods.add(new MethodComplexity(name, ccn, params, start, end));
    }
}
