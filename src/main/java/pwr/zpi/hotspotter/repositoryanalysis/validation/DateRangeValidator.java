package pwr.zpi.hotspotter.repositoryanalysis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;

import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, RepositoryAnalysisController.AnalysisRequest> {

    private static final LocalDate MIN_START_DATE = LocalDate.of(2005, 1, 1);

    @Override
    public boolean isValid(RepositoryAnalysisController.AnalysisRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return true;
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (request.startDate().isBefore(MIN_START_DATE)) {
            context.buildConstraintViolationWithTemplate("Start date cannot be before 2005-01-01")
                    .addConstraintViolation();
            isValid = false;
        }

        if (request.startDate().isAfter(request.endDate())) {
            context.buildConstraintViolationWithTemplate("Invalid date range: start date must be before or equal to end date")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

}
