package com.ru.scraper.store.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.store.models.ExecutionState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public void saveSuccessfulExecution(LocalDateTime executionTime, String ruCode) {
        try {
            ExecutionState state = new ExecutionState(
                    "SUCCEEDED",
                    utils.getFullDateTime(executionTime),
                    ruCode
            );

            dynamoDBMapper.save(state);
            System.out.println("Saved SUCCEEDED state to DynamoDB for ruCode: " + ruCode);
        } catch (Exception e) {
            System.err.println("Failed to save successful execution state: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save execution state", e);
        }
    }

    public void saveFailedExecution(LocalDateTime executionTime, String errorMessage, String ruCode) {
        try {
            ExecutionState state = new ExecutionState(
                    "FAILED",
                    utils.getFullDateTime(executionTime),
                    ruCode,
                    errorMessage
            );

            dynamoDBMapper.save(state);
            System.out.println("Saved FAILED state to DynamoDB for ruCode: " + ruCode);
        } catch (Exception e) {
            System.err.println("Failed to save failed execution state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isScrapingNeeded(String ruCode, LocalDateTime nextDayDate) {
        try {
            System.out.println("Checking scraping status for ruCode: " + ruCode + " and date: " + utils.getFormattedDate(nextDayDate));

            ExecutionState lastExecution = getLastExecutionForDateAndRuCode(ruCode, nextDayDate);

            if (lastExecution == null) {
                System.out.println("No previous execution found for ruCode: " + ruCode + " and date: " + utils.getFormattedDate(nextDayDate));
                return true;
            }

            boolean isNeeded = !"SUCCEEDED".equals(lastExecution.getStatus());

            System.out.println("Last execution for ruCode: " + ruCode + " and date: " + utils.getFormattedDate(nextDayDate) +
                    " was: " + lastExecution.getStatus() +
                    ". Scraping needed: " + isNeeded);

            if (!isNeeded) {
                System.out.println("Skipping scraping - already successful for this date and restaurant");
            }

            return isNeeded;

        } catch (Exception e) {
            System.err.println("Error checking if scraping is needed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private ExecutionState getLastExecutionForDateAndRuCode(String ruCode, LocalDateTime localDate) {
        try {
            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

            String formattedDate = utils.getFormattedDate(localDate); // "20/06/2025"
            System.out.println("Scanning DynamoDB for ruCode: " + ruCode + " and date prefix: " + formattedDate);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":ruCode", new AttributeValue().withS(ruCode));
            expressionAttributeValues.put(":execution_date", new AttributeValue().withS(formattedDate));

            scanExpression.setFilterExpression("ru_code = :ruCode AND begins_with(execution_time, :execution_date)");
            scanExpression.setExpressionAttributeValues(expressionAttributeValues);

            System.out.println("Executing DynamoDB scan...");
            List<ExecutionState> executions = dynamoDBMapper.scan(ExecutionState.class, scanExpression);
            System.out.println("DynamoDB scan completed. Found " + executions.size() + " records");

            return executions.stream()
                    .max((e1, e2) -> e1.getExecutionTime().compareTo(e2.getExecutionTime()))
                    .orElse(null);

        } catch (Exception e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}