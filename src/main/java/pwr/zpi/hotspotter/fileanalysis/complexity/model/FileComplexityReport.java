package pwr.zpi.hotspotter.fileanalysis.complexity.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileComplexityReport {

    private int totalCCN;
    private double averageCCN;
    private int maxCCN;
    private int methodsCount;
    private List<MethodComplexity> methods = new ArrayList<>();

    public void updateCcnStats(int ccn) {
        this.totalCCN += ccn;
        this.methodsCount++;
        this.maxCCN = Math.max(this.maxCCN, ccn);
        this.averageCCN = (double) totalCCN / methodsCount;
    }

    public void addMethodStats(String name, int ccn, int params, int start, int end) {
        updateCcnStats(ccn);
        this.methods.add(new MethodComplexity(name, ccn, params, start, end));
    }

}
