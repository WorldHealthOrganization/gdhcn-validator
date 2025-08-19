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
1. Set GitHub variable: `HEROKU_APP_NAME=your-app`
2. Add secrets: `HEROKU_API_KEY`, `HEROKU_EMAIL`

### For Signed Releases  
Add these GitHub secrets:
- `KEY_ALIAS`, `KEY_PASSWORD`, `KEY_STORE_PASSWORD`, `SIGNING_KEY`

## Artifacts Location
- **GitHub Actions** → Workflow runs → Artifacts section
- **Releases** → GitHub Releases page  
- **Live previews** → Check PR comments for links