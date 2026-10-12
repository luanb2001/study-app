package com.studyapp.backend.api;

import com.studyapp.backend.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public SyncService.SyncResponse synchronize(
            @Valid @RequestBody SyncService.SyncRequest request) {
        return this.syncService.synchronize(request);
    }
}
