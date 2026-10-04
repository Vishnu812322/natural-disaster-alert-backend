# System Architecture

The system uses a central Spring Boot API with MySQL, a React/Vite web client, and an Android client added later.

## Alert lifecycle

AUTHORIZED SOURCE -> INGESTION -> VALIDATION -> NORMALIZATION -> GEO TARGETING -> NOTIFICATION DISPATCH -> DELIVERY LOG -> UPDATE/EXPIRY

## Trust boundary

The application must distinguish:
- official/authorized source alerts
- administrator-authored alerts
- development/test alerts

A test alert must never be represented as an official emergency warning.
