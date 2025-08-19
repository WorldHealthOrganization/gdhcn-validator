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

### For Heroku Review Apps
1. Create Heroku app: `heroku create your-app-name`
2. Enable Review Apps in Heroku Dashboard
3. Set GitHub variable: `HEROKU_APP_NAME=your-app-name`
4. Add secrets: `HEROKU_API_KEY`, `HEROKU_EMAIL`

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