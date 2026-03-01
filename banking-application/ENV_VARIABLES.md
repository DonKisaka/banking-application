# Environment Variables for Azure Deployment

Set these in your Azure App Service (or other host) when using the `azure` profile.

## Required (Azure profile)

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `azure` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL (use `?sslmode=require` for Azure) | `jdbc:postgresql://your-server.postgres.database.azure.com:5432/hdfc_banking?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `bankingsystem_admin` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | *(your password)* |
| `SPRING_DATA_REDIS_HOST` | Azure Redis host | `your-cache.redis.cache.windows.net` |
| `SPRING_DATA_REDIS_PASSWORD` | Azure Redis access key | *(your Redis key)* |

## Optional (all environments)

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATA_REDIS_PORT` | Redis port (Azure often uses 6380) | `6380` (azure) / `6379` (local) |
| `SPRING_DATA_REDIS_SSL_ENABLED` | Enable Redis SSL | `false` (local) / `true` (azure) |
| `JWT_SECRET_KEY` | Secret for JWT signing (use a strong value in production) | *(dev default in code)* |
| `JWT_EXPIRATION_TIME` | Access token expiry (ms) | `3600000` |
| `JWT_REFRESH_EXPIRATION` | Refresh token expiry (ms) | `86400000` |
| `SERVER_PORT` | Server port | `8082` |
| `SCHEDULING_ENABLED` | Enable scheduled jobs | `true` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | DB connection pool size | `5` |
| `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | DB connection timeout (ms) | `30000` |

## Setting in Azure App Service

1. **Azure Portal**: App Service → **Configuration** → **Application settings** → **New application setting**.
2. **Azure CLI**:
   ```bash
   az webapp config appsettings set --resource-group <rg> --name <app-name> --settings \
     SPRING_PROFILES_ACTIVE=azure \
     SPRING_DATASOURCE_URL="jdbc:postgresql://..." \
     SPRING_DATASOURCE_USERNAME="..." \
     SPRING_DATASOURCE_PASSWORD="..." \
     SPRING_DATA_REDIS_HOST="..." \
     SPRING_DATA_REDIS_PASSWORD="..."
   ```

Never commit real passwords or keys to the repo; use Azure Key Vault or App Service managed identity where possible.
