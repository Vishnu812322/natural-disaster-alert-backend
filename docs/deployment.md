# Deployment

Development:
- MySQL local or docker-compose
- Spring Boot on 8080
- React/Vite on 5173

Production:
- Use HTTPS
- Store secrets in a secret manager/environment
- Use a managed database
- Configure an authorized disaster-data feed
- Configure real SMS provider
- Configure FCM
- Restrict admin roles
- Enable monitoring and backups
- Do not log OTPs or provider credentials
- Separate TEST and PRODUCTION environments
