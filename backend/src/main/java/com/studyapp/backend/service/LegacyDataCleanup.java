package com.studyapp.backend.service;

import com.studyapp.backend.repository.LegacyDataRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LegacyDataCleanup implements ApplicationRunner {
    private final LegacyDataRepository legacyDataRepository;

    public LegacyDataCleanup(LegacyDataRepository legacyDataRepository) {
        this.legacyDataRepository = legacyDataRepository;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        this.legacyDataRepository.dropLegacyGlobalReviewSubjectConstraint();
        this.legacyDataRepository.removeUnownedRecords();
    }
}
