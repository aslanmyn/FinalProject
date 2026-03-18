package ru.kors.finalproject.service;

import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.FinalGrade;

import java.util.Collection;
import java.util.Objects;

@Service
public class GpaCalculationService {

    public double calculatePublishedGpa(Collection<FinalGrade> finalGrades) {
        if (finalGrades == null || finalGrades.isEmpty()) {
            return 0.0;
        }
        return calculateWeightedGpa(finalGrades.stream()
                .filter(Objects::nonNull)
                .filter(FinalGrade::isPublished)
                .filter(finalGrade -> finalGrade.getSubjectOffering() != null
                        && finalGrade.getSubjectOffering().getSubject() != null)
                .map(finalGrade -> new CreditPoints(
                        finalGrade.getPoints(),
                        finalGrade.getSubjectOffering().getSubject().getCredits()
                ))
                .toList());
    }

    public double calculateWeightedGpa(Collection<CreditPoints> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }

        double totalWeightedPoints = 0.0;
        int totalCredits = 0;

        for (CreditPoints item : items) {
            if (item == null || item.credits() <= 0) {
                continue;
            }
            totalWeightedPoints += item.points() * item.credits();
            totalCredits += item.credits();
        }

        return totalCredits == 0 ? 0.0 : totalWeightedPoints / totalCredits;
    }

    public record CreditPoints(double points, int credits) {
    }
}
