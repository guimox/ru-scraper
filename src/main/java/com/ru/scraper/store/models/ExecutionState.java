package com.ru.scraper.store.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEpochDate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@DynamoDBTable(tableName = "ru-scraper-executions")
public class ExecutionState {

    private String executionId;
    private String status;
    private String ruCode;
    private String executionTime;
    private String errorMessage;
    private String runType;
    private Date expiresAt;

    // No-arg constructor (required by DynamoDB)
    public ExecutionState() {}

    // 4-parameter constructor for successful executions
    public ExecutionState(String status, String executionTime, String ruCode, String runType) {
        this.executionId = java.util.UUID.randomUUID().toString();
        this.status = status;
        this.executionTime = executionTime;
        this.ruCode = ruCode;
        this.runType = runType;
        this.expiresAt = Date.from(LocalDateTime.now().plusDays(5).toInstant(ZoneOffset.UTC));
    }

    // 5-parameter constructor for failed executions (includes errorMessage)
    public ExecutionState(String status, String executionTime, String ruCode, String runType, String errorMessage) {
        this.executionId = java.util.UUID.randomUUID().toString();
        this.status = status;
        this.executionTime = executionTime;
        this.ruCode = ruCode;
        this.runType = runType;
        this.errorMessage = errorMessage;
        this.expiresAt = Date.from(LocalDateTime.now().plusDays(5).toInstant(ZoneOffset.UTC));
    }

    @DynamoDBHashKey(attributeName = "id")
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @DynamoDBAttribute(attributeName = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDBAttribute(attributeName = "execution_time")
    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    @DynamoDBAttribute(attributeName = "error_message")
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @DynamoDBAttribute(attributeName = "ru_code")
    public String getRuCode() {
        return ruCode;
    }

    public void setRuCode(String ruCode) {
        this.ruCode = ruCode;
    }

    @DynamoDBAttribute(attributeName = "run_type")
    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    @DynamoDBTypeConvertedEpochDate
    @DynamoDBAttribute(attributeName = "expires_at")
    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}