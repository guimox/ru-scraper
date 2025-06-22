package com.ru.scraper.store.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.store.models.ExecutionState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutionStateService {

    private final DynamoDBMapper dynamoDBMapper;
    private final Utils utils;

    public ExecutionStateService(Utils utils, DynamoDBMapper dynamoDBMapper) {
        this.utils = utils;
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void saveSuccessfulExecution(LocalDateTime executionTime, String ruCode, String runType) {
        try {
            // Ensure we're using UTC time
            LocalDateTime utcExecutionTime = executionTime; // Should already be UTC from main method

            ExecutionState state = new ExecutionState(
                    "SUCCEEDED",
                    utils.getFullDateTime(utcExecutionTime),
                    ruCode,
                    runType
            );

            dynamoDBMapper.save(state);
            System.out.println("Saved SUCCEEDED state to DynamoDB for ruCode: " + ruCode +
                    " with runType: " + runType + " at UTC time: " + utils.getFullDateTime(utcExecutionTime));
        } catch (Exception e) {
            System.err.println("Failed to save successful execution state: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save execution state", e);
        }
    }

    public void saveFailedExecution(LocalDateTime executionTime, String errorMessage, String ruCode, String runType) {
        try {
            ExecutionState state = new ExecutionState(
                    "FAILED",
                    utils.getFullDateTime(executionTime),
                    ruCode,
                    runType,
                    errorMessage
            );

            dynamoDBMapper.save(state);
            System.out.println("Saved FAILED state to DynamoDB for ruCode: " + ruCode +
                    " with runType: " + runType + " at UTC time: " + utils.getFullDateTime(executionTime));
        } catch (Exception e) {
            System.err.println("Failed to save failed execution state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isScrapingNeeded(String ruCode, LocalDateTime targetDate) {
        try {
            String targetDateStr = utils.getFormattedDate(targetDate);
            System.out.println("Checking if BACKUP scraping is needed for ruCode: " + ruCode +
                    " for target date: " + targetDateStr + " (UTC: " + targetDate + ")");

            // Look for successful PRIMARY execution for this target date within the last 24 hours
            ExecutionState lastPrimaryExecution = getLastPrimaryExecutionForTargetDate(ruCode, targetDate);

            if (lastPrimaryExecution == null) {
                System.out.println("No successful PRIMARY execution found for ruCode: " + ruCode +
                        " and target date: " + targetDateStr + " - BACKUP run needed");
                return true;
            }

            if ("SUCCEEDED".equals(lastPrimaryExecution.getStatus())) {
                System.out.println("Found successful PRIMARY execution for ruCode: " + ruCode +
                        " and target date: " + targetDateStr +
                        " at: " + lastPrimaryExecution.getExecutionTime() + " - BACKUP run not needed");
                return false;
            }

            System.out.println("PRIMARY execution failed for ruCode: " + ruCode +
                    " and target date: " + targetDateStr + " - BACKUP run needed");
            return true;

        } catch (Exception e) {
            System.err.println("Error checking if scraping is needed: " + e.getMessage());
            e.printStackTrace();
            // Return true on error to be safe and attempt scraping
            return true;
        }
    }

    private ExecutionState getLastPrimaryExecutionForTargetDate(String ruCode, LocalDateTime targetDate) {
        try {
            LocalDateTime lookbackStart = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);
            String lookbackStartStr = utils.getFormattedDate(lookbackStart);
            String currentDateStr = utils.getFormattedDate(LocalDateTime.now(ZoneOffset.UTC));

            System.out.println("Scanning DynamoDB for PRIMARY executions for ruCode: " + ruCode +
                    " between dates: " + lookbackStartStr + " and " + currentDateStr);

            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":ruCode", new AttributeValue().withS(ruCode));
            expressionAttributeValues.put(":runType", new AttributeValue().withS("PRIMARY"));

            scanExpression.setFilterExpression("ru_code = :ruCode AND run_type = :runType");
            scanExpression.setExpressionAttributeValues(expressionAttributeValues);

            System.out.println("Executing DynamoDB scan for PRIMARY executions...");
            List<ExecutionState> primaryExecutions = dynamoDBMapper.scan(ExecutionState.class, scanExpression);
            System.out.println("DynamoDB scan completed. Found " + primaryExecutions.size() + " PRIMARY execution records");

            return primaryExecutions.stream()
                    .filter(execution -> {
                        String executionTimeStr = execution.getExecutionTime();

                        // Check if this execution happened within the last 24 hours
                        boolean isRecentExecution = executionTimeStr.startsWith(currentDateStr) ||
                                executionTimeStr.startsWith(lookbackStartStr);

                        if (isRecentExecution) {
                            System.out.println("Found recent PRIMARY execution: " + executionTimeStr +
                                    " with status: " + execution.getStatus());
                        }

                        return isRecentExecution;
                    })
                    .max((e1, e2) -> e1.getExecutionTime().compareTo(e2.getExecutionTime()))
                    .orElse(null);

        } catch (Exception e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}