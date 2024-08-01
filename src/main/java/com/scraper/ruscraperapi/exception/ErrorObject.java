package com.scraper.ruscraperapi.exception;

import java.io.Serializable;
import java.util.Date;

public class ErrorObject implements Serializable {
    private String message;
    private Date timestamp;

    public ErrorObject() {
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}