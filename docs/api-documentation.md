# API quick reference

Public:
GET /api/health
GET /api/public/alerts

Auth:
POST /api/auth/login
POST /api/auth/verify-otp

Admin:
GET /api/alerts
GET /api/alerts/active
POST /api/alerts
POST /api/alerts/{id}/resolve
GET /api/dashboard/summary

Devices:
POST /api/devices

Notifications:
POST /api/notifications/alert/{id}/prepare

CAP:
POST /api/cap/parse
