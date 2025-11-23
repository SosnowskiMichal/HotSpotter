package pwr.zpi.hotspotter.repositoryanalysis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, RepositoryAnalysisController.AnalysisRequest> {

    @Override
    public boolean isValid(RepositoryAnalysisController.AnalysisRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return true;
        }
        return !request.startDate().isAfter(request.endDate());
    }

}
