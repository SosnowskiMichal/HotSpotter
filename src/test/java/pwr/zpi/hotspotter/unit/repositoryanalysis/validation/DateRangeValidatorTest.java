package pwr.zpi.hotspotter.unit.repositoryanalysis.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;
import pwr.zpi.hotspotter.repositoryanalysis.validation.DateRangeValidator;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateRangeValidatorTest {

    private DateRangeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();

        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    @Test
    void shouldReturnTrueWhenRequestIsNull() {
        boolean result = validator.isValid(null, context);

        assertTrue(result);
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnTrueWhenStartDateIsNull() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        null,
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnTrueWhenEndDateIsNull() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2024, 1, 1),
                        null
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnTrueForValidDateRange() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnTrueWhenStartDateEqualsEndDate() {
        LocalDate sameDate = LocalDate.of(2024, 6, 15);
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        sameDate,
                        sameDate
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnTrueWhenStartDateIsExactlyMinDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2005, 1, 1),
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnFalseWhenStartDateIsBeforeMinDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2004, 12, 31),
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Start date cannot be before 2005-01-01"
        );
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldReturnFalseWhenStartDateIsAfterEndDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2024, 12, 31),
                        LocalDate.of(2024, 1, 1)
                );

        boolean result = validator.isValid(request, context);

        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Invalid date range: start date must be before or equal to end date"
        );
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldReturnFalseWithMultipleViolations() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2004, 12, 31),
                        LocalDate.of(2004, 6, 1)
                );

        boolean result = validator.isValid(request, context);

        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Start date cannot be before 2005-01-01"
        );
        verify(context).buildConstraintViolationWithTemplate(
                "Invalid date range: start date must be before or equal to end date"
        );
        verify(violationBuilder, times(2)).addConstraintViolation();
    }

    @Test
    void shouldReturnFalseWhenStartDateIsOneDayBeforeMinDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2004, 12, 31),
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenStartDateIsOneDayAfterMinDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2005, 1, 2),
                        LocalDate.of(2024, 12, 31)
                );

        boolean result = validator.isValid(request, context);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenStartDateIsOneDayAfterEndDate() {
        RepositoryAnalysisController.AnalysisRequest request =
                new RepositoryAnalysisController.AnalysisRequest(
                        "url",
                        LocalDate.of(2024, 6, 16),
                        LocalDate.of(2024, 6, 15)
                );

        boolean result = validator.isValid(request, context);

        assertFalse(result);
    }
}