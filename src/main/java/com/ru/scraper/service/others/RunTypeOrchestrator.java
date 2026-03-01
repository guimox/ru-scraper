package com.ru.scraper.service.others;

import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.exception.types.CheckupMenuChangedException;
import com.ru.scraper.exception.types.CheckupNoChangesException;
import com.ru.scraper.service.types.BackupRunService;
import com.ru.scraper.service.types.CheckupRunService;
import com.ru.scraper.service.types.PrimaryRunService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RunTypeOrchestrator {

    private final BackupRunService backupRunService;
    private final CheckupRunService checkupRunService;
    private final PrimaryRunService primaryRunService;

    public RunTypeOrchestrator(BackupRunService backupRunService, CheckupRunService checkupRunService,
                             PrimaryRunService primaryRunService) {
        this.backupRunService = backupRunService;
        this.checkupRunService = checkupRunService;
        this.primaryRunService = primaryRunService;
    }

    public Object executeRun(RunType runType, String ruCode, LocalDateTime targetDateTime,
                            LocalDateTime triggerDateTime) {
        switch (runType) {
            case BACKUP:
                return handleBackupRun(ruCode, targetDateTime, triggerDateTime, runType);
            case CHECKUP:
                return handleCheckupRun(ruCode, targetDateTime, triggerDateTime, runType);
            case PRIMARY:
            default:
                return handlePrimaryRun(ruCode, targetDateTime, triggerDateTime, runType);
        }
    }

    private Object handleBackupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        try {
            backupRunService.executeBackupRun(ruCode, targetDateTime, triggerDateTime, runType);
        } catch (BackupRunService.BackupNotNeedException e) {
            return e.getMessage();
        }

        return handlePrimaryRun(ruCode, targetDateTime, triggerDateTime, runType);
    }

    private Object handleCheckupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("CHECKUP run - performing health check...");
        try {
            checkupRunService.executeCheckupRun(ruCode, targetDateTime, triggerDateTime, runType);
        } catch (CheckupMenuChangedException e) {
            return e.getMenu();
        } catch (CheckupNoChangesException e) {
            System.out.println("CHECKUP: " + e.getMessage());
            return e.getMessage();
        }
        return null;
    }

    private Object handlePrimaryRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("PRIMARY run - proceeding without checking previous executions...");
        return primaryRunService.executePrimaryRun(ruCode, targetDateTime, triggerDateTime, runType);
    }
}

