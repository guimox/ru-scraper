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
        ExecutionState state = new ExecutionState(
                "SUCCEEDED",
                utils.getFormattedDateTime(executionTime),
                ruCode
        );

        dynamoDBMapper.save(state);
        System.out.println("Saved SUCCEEDED state to DynamoDB for ruCode: " + ruCode);
    }

    public void saveFailedExecution(LocalDateTime executionTime, String errorMessage, String ruCode) {
        ExecutionState state = new ExecutionState(
                "FAILED",
                utils.getFormattedDateTime(executionTime),
                ruCode,
                errorMessage
        );

        dynamoDBMapper.save(state);
        System.out.println("Saved FAILED state to DynamoDB for ruCode: " + ruCode);
    }

    public boolean isScrapingNeeded(String ruCode, LocalDateTime nextDayDate) {
        ExecutionState lastExecution = getLastExecutionForDateAndRuCode(ruCode, nextDayDate);

        if (lastExecution == null) {
            System.out.println("No previous execution found for ruCode: " + ruCode + " and date: " + utils.getFormattedDateTime(nextDayDate));
            return true;
        }

        boolean isNeeded = !"SUCCEEDED".equals(lastExecution.getStatus());

        System.out.println("Last execution for ruCode: " + ruCode + " and date: " + utils.getFormattedDateTime(nextDayDate) +
                " was: " + lastExecution.getStatus() +
                ". Scraping needed: " + isNeeded);

        if (!isNeeded) {
            System.out.println("Skipping scraping - already successful for this date and restaurant");
        }

        return isNeeded;
    }

    private ExecutionState getLastExecutionForDateAndRuCode(String ruCode, LocalDateTime localDate) {
        String formattedDate = utils.getFormattedDateTime(localDate);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":ruCode", new AttributeValue().withS(ruCode));
        expressionAttributeValues.put(":execution_time", new AttributeValue().withS(formattedDate));

        scanExpression.setFilterExpression("ru_code = :ruCode AND execution_time = :execution_time");
        scanExpression.setExpressionAttributeValues(expressionAttributeValues);

        List<ExecutionState> executions = dynamoDBMapper.scan(ExecutionState.class, scanExpression);

        return executions.stream()
                .max((e1, e2) -> e1.getExecutionTime().compareTo(e2.getExecutionTime()))
                .orElse(null);
    }
}