# Quick Deployment Reference

> **📖 For complete documentation, see [docs/deployment.md](deployment.md)**

## Preview Branch Deployment

### Automatic (Pull Requests)
- ✨ **Just create a PR** - deployment happens automatically
- 📱 Get Android APK and Web WAR artifacts  
- 🌐 Optional live preview on Heroku Review Apps
- 💬 Automatic PR comment with download links

### Manual (Feature Branches)
```bash
# Deploy any branch for testing
git push origin feature/my-feature
git push origin preview/my-feature

# Triggers automatic build and artifact creation
```

## Main Branch Deployment
```bash
# Deploy to staging/latest
git push origin main

# Creates "latest" artifacts with 30-day retention
```

## Production Release
```bash
# 1. Update version in app/build.gradle
# 2. Commit and tag
git tag v1.2.3
git push origin main && git push origin v1.2.3

# Creates signed APK, AAB, and WAR files
```

## Quick Setup

## Quick Setup

### For Heroku Review Apps
1. **Heroku Setup**:
   ```bash
   heroku create your-app-name
   heroku buildpacks:set heroku/java
   ```
2. **Enable Review Apps**: Go to Heroku Dashboard → Deploy → Enable Review Apps
3. **GitHub Secrets** (Settings → Secrets and variables → Actions → Secrets):
   - `HEROKU_API_KEY` = `heroku auth:token` output
   - `HEROKU_EMAIL` = your Heroku account email
4. **GitHub Variables** (Settings → Secrets and variables → Actions → Variables):
   - `HEROKU_APP_NAME` = `your-app-name` (base name only)

### GitHub Configuration (Detailed)
```bash
# Get required info
heroku auth:token    # Copy this for HEROKU_API_KEY
heroku auth:whoami   # Copy this for HEROKU_EMAIL

# Repository Settings → Secrets and variables → Actions
# Add Secrets:
#   HEROKU_API_KEY = <token from above>
#   HEROKU_EMAIL = <email from above>
# Add Variables:
#   HEROKU_APP_NAME = your-app-name
```

### Alternative: GitHub CLI Setup
```bash
gh secret set HEROKU_API_KEY --body "your-api-key"
gh secret set HEROKU_EMAIL --body "your-email@example.com"
gh variable set HEROKU_APP_NAME --body "your-app-name"
```

### Essential Heroku Commands
```bash
# Setup
heroku login
heroku create your-app-name
heroku buildpacks:set heroku/java

# Deploy manually
git push heroku main

# Monitor
heroku logs --tail
heroku ps
heroku apps:info your-app-name

# Review Apps
heroku apps                    # List all apps
heroku apps:destroy app-name   # Clean up old review apps
```

### For Signed Releases  
Add these GitHub secrets:
- `KEY_ALIAS`, `KEY_PASSWORD`, `KEY_STORE_PASSWORD`, `SIGNING_KEY`

## Artifacts Location
- **GitHub Actions** → Workflow runs → Artifacts section
- **Releases** → GitHub Releases page  
- **Live previews** → Check PR comments for links