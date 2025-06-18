# RU Scraper

## Overview

RU Scraper is a component of the [RU Menu project](https://github.com/guimox/ru-menu) that handles web scraping of university restaurant (RU) menus. It's built as a Spring Boot application deployed as an AWS Lambda function.

## Features

- Scrapes menu information from university restaurant websites
- Supports both table-based and image-based menu formats
- Stores execution states in DynamoDB for tracking successful/failed scraping attempts
- Runs on a scheduled basis via AWS EventBridge
- Provides REST API endpoint for manual triggering

## Architecture

- **Spring Boot Application**: Core application built with Java 17
- **AWS Lambda**: Serverless deployment
- **DynamoDB**: Stores execution states
- **AWS EventBridge**: Handles scheduled execution

## Tech Stack

- Java 17
- Spring Boot 3.1.3
- Spring Cloud Function
- AWS SDK
- JSoup for web scraping
- Maven for build management

## API Endpoint (local)

```http
GET /api/scraper/menu?targetDateOffset={days}
```

- `targetDateOffset`: Number of days ahead to scrape (default: 1)
- Works just locally/in dev env because in prod, the Spring Cloud Function takes over

## Deployment

The project uses AWS CodeBuild for CI/CD:

- Builds with Maven profile `prd`
- Deploys to multiple Lambda functions based on branch:
  - `homolog` branch: Deploys to testing environment only
  - Other branches: Deploys to all production environments

## Project Structure

```
ru-scraper/
├── src/
│   ├── main/java/com/ru/scraper/
│   │   ├── controller/      # REST endpoints
│   │   ├── data/           # Data models
│   │   ├── exception/      # Exception handling
│   │   ├── factory/        # Response builders
│   │   ├── helper/         # Utility classes
│   │   ├── scraper/        # Core scraping logic
│   │   ├── service/        # Business logic
│   │   └── store/          # DynamoDB integration
│   └── test/              # Test cases
└── infra/                 # Infrastructure configuration
    ├── lambda/           # AWS Lambda configuration
    └── buildspec.yml     # AWS CodeBuild specification
```

## Related Projects

- [RU Menu](https://github.com/guimox/ru-menu) - Main project repository
