# Natural Disaster Early Warning & Mobile Alert System

A real-world oriented disaster-alert platform built in phases:

1. Spring Boot + MySQL foundation
2. Authentication + OTP framework
3. Disaster alert management
4. Admin dashboard
5. Geographic targeting
6. CAP XML ingestion/parser
7. Push/SMS notification interfaces
8. Public alert website
9. Android mobile client
10. Tests, Docker and deployment documentation

## Important operational note

This project is an alerting platform, not a replacement for India's official cell-broadcast/emergency-warning infrastructure.
It is designed to consume authorized/official feeds where access is available and to deliver alerts to registered users/devices.

No government alert feed is fabricated in this repository. Development mode provides an explicitly marked TEST alert path.

## Requirements

- Java 21
- Maven 3.9+
- Node.js 20+
- npm
- MySQL 8+
- Git (optional)
- Android Studio for the mobile client

## Quick start - backend

Create a MySQL database:

    CREATE DATABASE disaster_alert;

Set environment variables (Windows PowerShell example):

    $env:DB_URL="jdbc:mysql://localhost:3306/disaster_alert?useSSL=false&serverTimezone=UTC"
    $env:DB_USERNAME="root"
    $env:DB_PASSWORD="your_password"
    $env:JWT_SECRET="change-this-to-a-long-random-secret-at-least-32-chars"

Run:

    cd backend
    mvn spring-boot:run

Backend:
    http://localhost:8080

Health:
    http://localhost:8080/api/health

## Quick start - frontend

    cd frontend
    npm install
    npm run dev

Frontend:
    http://localhost:5173

The frontend uses VITE_API_BASE_URL if supplied, otherwise http://localhost:8080/api.

## Development authentication

For safe local development, OTP delivery defaults to the console provider. The API prints the OTP in the backend console.
For production, replace the provider implementation with a real SMS/email provider and never log OTPs.

Default development admin:
    admin@example.com / Admin@12345

Change this immediately for any real deployment.

## Real integrations

Configure real services only after validating their terms/API access:

- Official disaster/CAP feed: configure source URL in the database/admin layer.
- Firebase Cloud Messaging: provide Firebase service-account configuration and implement/enable the FCM provider.
- SMS: configure an approved SMS provider.
- Maps/geocoding: use an appropriate production map provider and API key.

Do not hard-code secrets.

## Project structure

- backend: Spring Boot REST API
- frontend: React/Vite public site + admin console
- mobile: Android client consuming the same backend
- database: SQL reference/schema
- docs: architecture, API, deployment and operations
- deployment: Docker/nginx examples

## Test mode

The admin API supports creating an alert marked TEST. Test alerts are clearly labeled and should only be sent to development/test devices.
Never use test alerts to represent a real public emergency.


## Phase 4 implemented

The web frontend now includes:
- Control-center admin dashboard
- Active alert statistics
- Live Leaflet map
- Alert radius visualization
- Alert management table
- Resolve alert action
- Structured test-alert creation form
- Clear TEST/production distinction
- Public live alert map
- Public active-alert cards
- Automatic public refresh
- Responsive admin layout
- Navigation placeholders for notification, device, source, safety and audit modules

Database initialization is now explicit through `schema.sql` and Spring SQL initialization. Hibernate uses `ddl-auto=validate`, preventing the earlier missing-table problem from being hidden.


## Phase 5 — Real CAP source ingestion

The project now contains a server-side NDMA SACHET CAP RSS ingestion pipeline. It uses the government-published India CAP RSS URL, ETag-aware polling, CAP XML retrieval, normalization, deduplication, and a Data Sources admin screen.

Default source:
`https://sachet.ndma.gov.in/cap_public_website/rss/rss_india.xml`

The source is created automatically in the `feed_sources` table on first startup. The scheduler checks enabled CAP RSS sources every 5 minutes. You can also open **Admin → Data Sources → Sync now** to trigger a manual synchronization.

The official NDMA integration guide requires ETag caching; this implementation sends `If-None-Match` after the first successful response and handles HTTP 304 without downloading the unchanged feed again.

The ingestion code is deliberately conservative: if a CAP item cannot be retrieved or parsed, it is not turned into a fabricated alert.
