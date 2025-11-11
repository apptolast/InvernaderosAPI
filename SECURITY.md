# Security Guide - InvernaderosAPI

## 🔒 Overview

This document outlines security best practices and guidelines for the InvernaderosAPI project.

## ⚠️ NEVER Commit Credentials

**CRITICAL:** Never commit any of the following to version control:

- ❌ Passwords
- ❌ API keys
- ❌ Access tokens
- ❌ Private SSH keys
- ❌ Database credentials
- ❌ MQTT credentials
- ❌ Redis passwords
- ❌ Any other secrets

## 📁 Protected Files

The following files are protected by `.gitignore` and should NEVER be committed:

- `.env`
- `.env.local`
- `.env.*.local`
- `application-local.yaml`
- `docker-compose.override.yaml`
- `*.key`
- `*.pem`
- `*.p12`
- `*.jks`
- `secrets/`
- `credentials/`

## 🔐 Environment Variables Setup

### Local Development

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` and set your local credentials:**
   ```bash
   # Generate secure passwords
   openssl rand -base64 32
   ```

3. **Set all required variables** in the `.env` file (see `.env.example` for template)

### Production/Staging

**NEVER use plaintext environment variables in production!**

Use a secure secret management solution:

#### Option 1: Kubernetes Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: invernaderos-api-secret
type: Opaque
data:
  postgres-password: <base64-encoded-value>
  redis-password: <base64-encoded-value>
  mqtt-password: <base64-encoded-value>
```

#### Option 2: AWS Secrets Manager
```bash
aws secretsmanager create-secret \
  --name invernaderos-api/database \
  --secret-string '{"password":"your-secure-password"}'
```

#### Option 3: HashiCorp Vault
```bash
vault kv put secret/invernaderos-api/database \
  password="your-secure-password"
```

#### Option 4: Azure Key Vault
```bash
az keyvault secret set \
  --vault-name invernaderos-vault \
  --name database-password \
  --value "your-secure-password"
```

## 🛡️ Security Best Practices

### Password Requirements

For production environments, use passwords that meet these criteria:

- ✅ Minimum 16 characters
- ✅ Mix of uppercase, lowercase, numbers, and special characters
- ✅ Generated using cryptographically secure random generator
- ✅ Unique for each service/environment
- ✅ Rotated regularly (every 90 days recommended)

### Generate Secure Passwords

```bash
# Generate a 32-character base64 password
openssl rand -base64 32

# Generate a 24-character alphanumeric password
openssl rand -base64 24 | tr -d "=+/" | cut -c1-24

# Generate using pwgen (install first: apt-get install pwgen)
pwgen -s 32 1
```

### Credential Rotation

1. **Schedule:** Rotate credentials every 90 days minimum
2. **Process:**
   - Generate new credentials
   - Update in secret manager
   - Update application configuration
   - Restart services
   - Revoke old credentials
   - Monitor for any failures

### Database Security

1. **Use separate credentials for each environment**
   - Development: `greenhouse_timeseries_dev`, `greenhouse_metadata_dev`
   - Production: `greenhouse_timeseries`, `greenhouse_metadata`

2. **Principle of Least Privilege**
   - Grant only necessary permissions
   - Use read-only users for analytics
   - Create separate users for migrations

3. **Connection Security**
   - Use SSL/TLS for database connections in production
   - Configure `spring.datasource.hikari.connection-test-query`
   - Set appropriate timeout values

### MQTT Security

1. **Authentication:**
   - Use unique credentials per client
   - Implement ACL (Access Control Lists)
   - Use client certificates in production

2. **Connection Security:**
   - Use TLS/SSL (port 8883) instead of plain TCP (port 1883)
   - Use WSS (WebSocket Secure) for browser clients
   - Configure proper cipher suites

3. **EMQX Configuration:**
   ```yaml
   # In production, enable authentication plugin
   authentication:
     - mechanism: password_based
       backend: postgresql
   
   # Enable ACL
   authorization:
     sources:
       - type: postgresql
   ```

### Redis Security

1. **Authentication:**
   - Always set a strong password
   - Use ACL for fine-grained access control (Redis 6+)

2. **Network Security:**
   - Bind to localhost or private network only
   - Use TLS for connections
   - Configure firewall rules

3. **Configuration:**
   ```bash
   # redis.conf
   requirepass <strong-password>
   bind 127.0.0.1 ::1
   protected-mode yes
   ```

## 🔍 Security Scanning

### Before Committing

1. **Scan for secrets:**
   ```bash
   # Install git-secrets
   git secrets --scan
   
   # Install truffleHog
   trufflehog git file://. --only-verified
   ```

2. **Check for exposed credentials:**
   ```bash
   grep -r "password\|secret\|key\|token" . --exclude-dir=.git --exclude-dir=node_modules
   ```

### Automated Scanning

The project uses:
- **CodeQL** - Automated security scanning in CI/CD
- **Dependabot** - Dependency vulnerability scanning
- **GitHub Secret Scanning** - Detects committed secrets

## 🚨 If Credentials Are Exposed

If you accidentally commit credentials:

1. **Immediately rotate the exposed credentials**
2. **Remove from git history:**
   ```bash
   # Use BFG Repo-Cleaner or git-filter-branch
   bfg --replace-text passwords.txt
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   ```
3. **Force push to remote** (coordinate with team):
   ```bash
   git push --force --all
   ```
4. **Notify the team**
5. **Check logs for unauthorized access**
6. **Document the incident**

## 📋 Security Checklist

Before deploying:

- [ ] All credentials are stored in secret managers
- [ ] No plaintext passwords in code or configuration
- [ ] `.env` file is in `.gitignore`
- [ ] Strong passwords are used (16+ characters)
- [ ] Credentials are unique per environment
- [ ] SSL/TLS is enabled for all connections
- [ ] Firewall rules are properly configured
- [ ] Security scanning is enabled
- [ ] Logs don't contain sensitive information
- [ ] Monitoring and alerting are configured

## 🔗 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Best Practices](https://docs.spring.io/spring-security/reference/features/index.html)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/security.html)
- [Redis Security](https://redis.io/docs/management/security/)
- [EMQX Security](https://www.emqx.io/docs/en/latest/security/security.html)

## 📞 Reporting Security Issues

If you discover a security vulnerability:

1. **DO NOT** create a public GitHub issue
2. Email the security team at: security@apptolast.com
3. Provide detailed information about the vulnerability
4. Allow time for the issue to be fixed before public disclosure

---

**Last Updated:** 2025-11-11
**Version:** 1.0.0
