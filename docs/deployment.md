# Deployment Guide

This document provides comprehensive guidance for deploying the GDHCN Validator application, covering both preview branch deployments and production releases.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Preview Branch Deployment](#preview-branch-deployment)
- [Main Branch Deployment](#main-branch-deployment)
- [Production Release Deployment](#production-release-deployment)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

## Overview

The GDHCN Validator consists of two main components:

1. **Android App** (`/app`) - Mobile application for QR code scanning and verification
2. **Web App** (`/web`) - Spring Boot web application providing web-based verification

Both components can be deployed independently or together depending on your needs.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │    Web App      │
│   (APK/AAB)     │    │  (Spring Boot)  │
└─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│   Play Store    │    │   Web Server    │
│   Distribution  │    │ (Heroku/Cloud)  │
└─────────────────┘    └─────────────────┘
```

## Preview Branch Deployment

Preview deployments allow testing changes before they are merged to main. The system automatically creates preview deployments for:

- **Pull Requests**: Automatic deployment on PR creation/updates
- **Feature Branches**: Manual deployment via branch push
- **Development Branches**: Continuous deployment for `develop` and `preview/**` branches

### Workflow Triggers

The preview deployment workflow (`.github/workflows/preview-deploy.yml`) is triggered by:

```yaml
on:
  pull_request:
    branches: [main]
    types: [opened, synchronize, reopened]
  push:
    branches:
      - main
      - develop
      - 'preview/**'
      - 'feature/**'
```

### What Gets Deployed

For each preview deployment:

1. **Android APK** (Debug build)
   - Built from current branch code
   - Available as GitHub Actions artifact
   - Retained for 7 days

2. **Web Application WAR**
   - Spring Boot WAR file
   - Can be deployed to Heroku Review Apps
   - Available as GitHub Actions artifact

### Accessing Preview Deployments

#### Via Pull Request Comments

When a PR is created or updated, the workflow automatically adds a comment with:

```markdown
## 🚀 Preview Deployment

**Branch:** `pr-123`

### Available Artifacts:
- 📱 [Android APK](https://github.com/repo/actions/runs/123)
- 🌐 [Web WAR](https://github.com/repo/actions/runs/123)

### Live Preview:
- 🌐 [Web App Preview](https://app-name-pr-123.herokuapp.com)

### Installation Instructions:
1. Download the APK from the artifacts above
2. Install on Android device: `adb install app-debug.apk`
3. For web app: Download WAR and run `java -jar web.war`
```

#### Via GitHub Actions Artifacts

1. Go to the GitHub Actions tab
2. Find the "Preview Branch Deployment" workflow run
3. Download artifacts from the run summary

### Manual Preview Deployment

To deploy a feature branch for testing:

1. **Push to a preview branch:**
   ```bash
   git checkout -b preview/my-feature
   git push origin preview/my-feature
   ```

2. **Or push to an existing feature branch:**
   ```bash
   git push origin feature/my-feature
   ```

The workflow will automatically build and deploy the preview.

## Main Branch Deployment

When code is pushed to the `main` branch, the system creates a "latest" deployment that can be used for staging or integration testing.

### Automatic Main Deployment

Every push to `main` triggers:

1. **Test execution** - Ensures code quality
2. **Android APK build** - Debug build for testing
3. **Web WAR build** - Production-ready WAR file
4. **Artifact upload** - Longer retention (30 days)
5. **Optional staging deployment** - If configured

### Accessing Main Deployments

Main branch deployments are available as GitHub Actions artifacts with extended retention:

- **Android APK**: `android-apk-main-latest`
- **Web WAR**: `web-war-main-latest`

## Production Release Deployment

Production releases are created through the existing tag-based workflow.

### Creating a Production Release

1. **Update version numbers** in `app/build.gradle`:
   ```gradle
   versionCode 23
   versionName "0.2.0"
   ```

2. **Commit and tag** the release:
   ```bash
   git add app/build.gradle
   git commit -m "Bump version to 0.2.0"
   git tag v0.2.0
   git push origin main
   git push origin v0.2.0
   ```

3. **Release workflow runs** automatically:
   - Builds signed APK and AAB files
   - Builds production WAR file
   - Creates GitHub release
   - Uploads all artifacts

4. **Manual steps**:
   - Download AAB from GitHub release
   - Upload to Google Play Console
   - Deploy WAR to production web server

### Production Artifacts

The release workflow creates:

- **`app-release-v0.2.0.apk`** - Signed Android APK
- **`app-release-v0.2.0.aab`** - Android App Bundle for Play Store
- **`web-release-v0.2.0.war`** - Production web application

## Configuration

### Required Secrets

For signed releases, configure these GitHub secrets:

```
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
KEY_STORE_PASSWORD=your_keystore_password
SIGNING_KEY=base64_encoded_keystore_file
```

### Optional Configuration

Configure these GitHub variables for enhanced functionality:

```
HEROKU_APP_NAME=your-app-name          # Enables Heroku Review Apps
PREVIEW_DEPLOY_ENABLED=true            # Enables custom preview deployment
STAGING_DEPLOY_ENABLED=true            # Enables staging deployment
```

Configure these GitHub secrets for external deployments:

```
HEROKU_API_KEY=your_heroku_api_key
HEROKU_EMAIL=your_heroku_email
```

### Heroku Deployment Setup

This section provides complete step-by-step instructions for deploying the GDHCN Validator web application to Heroku, including both manual deployment and automated Review Apps for pull requests.

#### Prerequisites

1. **Heroku Account**: Create a free account at [heroku.com](https://heroku.com)
2. **Heroku CLI**: Install from [devcenter.heroku.com/articles/heroku-cli](https://devcenter.heroku.com/articles/heroku-cli)
3. **Git**: Required for deployment
4. **GitHub Repository Access**: Admin access to configure secrets and variables

#### Step 1: Install and Setup Heroku CLI

```bash
# Install Heroku CLI (macOS)
brew tap heroku/brew && brew install heroku

# Install Heroku CLI (Ubuntu/Debian)
curl https://cli-assets.heroku.com/install-ubuntu.sh | sh

# Install Heroku CLI (Windows)
# Download installer from https://devcenter.heroku.com/articles/heroku-cli

# Login to Heroku
heroku login
```

#### Step 2: Create Main Heroku Application

```bash
# Create the main application
heroku create gdhcn-validator

# Or use a custom name
heroku create your-app-name

# Set Java runtime version
heroku config:set JAVA_TOOL_OPTIONS="-Xmx512m" --app your-app-name

# Verify the app was created
heroku apps:info your-app-name
```

#### Step 3: Configure Heroku Application

```bash
# Set up Java buildpack (if not automatically detected)
heroku buildpacks:set heroku/java --app your-app-name

# Configure environment variables
heroku config:set SPRING_PROFILES_ACTIVE=production --app your-app-name

# View current configuration
heroku config --app your-app-name
```

#### Step 4: Manual Deployment (One-time)

```bash
# Clone and deploy the repository manually
git clone https://github.com/WorldHealthOrganization/gdhcn-validator.git
cd gdhcn-validator

# Add Heroku remote
heroku git:remote -a your-app-name

# Deploy current branch
git push heroku main

# View logs
heroku logs --tail --app your-app-name

# Open the deployed application
heroku open --app your-app-name
```

#### Step 5: Enable Review Apps for Pull Requests

Review Apps automatically create temporary apps for each pull request, allowing you to test changes before merging.

##### 5.1: Enable Review Apps in Heroku Dashboard

1. Go to [dashboard.heroku.com](https://dashboard.heroku.com)
2. Select your main app (`your-app-name`)
3. Click on the **"Deploy"** tab
4. Scroll down to **"Review Apps"** section
5. Click **"Enable Review Apps"**
6. Choose **"Create new review apps for pull requests automatically"**
7. Set **"Destroy stale review apps automatically"** (recommended: after 5 days)
8. Click **"Enable Review Apps"**

##### 5.2: Connect GitHub Repository

1. In the same **"Deploy"** tab, find **"Deployment method"**
2. Click **"GitHub"** and connect your GitHub account
3. Search for and select **"WorldHealthOrganization/gdhcn-validator"**
4. Click **"Connect"**
5. **Do not** enable automatic deploys for main branch (we use GitHub Actions instead)

#### Step 6: Configure GitHub Repository

##### 6.1: Add GitHub Secrets

Go to your GitHub repository settings → Secrets and variables → Actions → Secrets:

```bash
# Get your Heroku API key
heroku auth:token
```

Add these secrets:
- **Name**: `HEROKU_API_KEY`
- **Value**: `your-heroku-api-key-from-above-command`

- **Name**: `HEROKU_EMAIL`  
- **Value**: `your-heroku-account-email@example.com`

##### 6.2: Add GitHub Variables

Go to your GitHub repository settings → Secrets and variables → Actions → Variables:

- **Name**: `HEROKU_APP_NAME`
- **Value**: `your-app-name` (the base app name without any suffixes)

#### Step 7: Test Review Apps

1. **Create a test pull request**:
   ```bash
   git checkout -b test-heroku-deployment
   echo "# Test deployment" >> README.md
   git add README.md
   git commit -m "Test: Heroku deployment"
   git push origin test-heroku-deployment
   ```

2. **Create PR on GitHub**:
   - Go to your repository on GitHub
   - Click "Compare & pull request"
   - Create the pull request

3. **Verify automatic deployment**:
   - GitHub Actions workflow should trigger automatically
   - Check the Actions tab for "Preview Branch Deployment" workflow
   - A comment should appear on the PR with deployment links
   - A new Heroku app should be created: `your-app-name-pr-123`

#### Step 8: Monitor and Manage Deployments

##### View Heroku Apps
```bash
# List all apps
heroku apps

# View specific app info
heroku apps:info your-app-name-pr-123

# View recent deployments
heroku releases --app your-app-name
```

##### Check Application Health
```bash
# View logs
heroku logs --tail --app your-app-name

# Check app status
heroku ps --app your-app-name

# Scale dynos (if needed)
heroku ps:scale web=1 --app your-app-name
```

##### Manual Review App Management
```bash
# Create a review app manually
heroku review-apps:create --app your-app-name --pr 123

# Destroy a review app
heroku apps:destroy your-app-name-pr-123 --confirm your-app-name-pr-123
```

#### Step 9: Production Deployment Configuration

For production deployments, configure additional settings:

```bash
# Set production environment
heroku config:set NODE_ENV=production --app your-app-name
heroku config:set SPRING_PROFILES_ACTIVE=production --app your-app-name

# Configure SSL (automatically enabled on paid plans)
heroku certs:auto:enable --app your-app-name

# Set up custom domain (optional)
heroku domains:add yourdomain.com --app your-app-name

# Configure logging
heroku logs --tail --app your-app-name
```

#### Troubleshooting Heroku Deployments

##### Common Issues and Solutions

**Issue**: Review App creation fails
```
Error: Could not create review app
```
**Solutions**:
1. Verify `HEROKU_API_KEY` secret is correct
2. Check that Review Apps are enabled in Heroku Dashboard
3. Ensure the base app exists and is accessible
4. Verify GitHub-Heroku connection is active

**Issue**: Build failure during deployment
```
Error: Failed to compile app
```
**Solutions**:
1. Check Heroku build logs: `heroku logs --app your-app-name-pr-123`
2. Verify `Procfile` is present and correct
3. Check `system.properties` specifies Java 17
4. Ensure `gradlew build` works locally

**Issue**: Application won't start
```
Error R10 (Boot timeout)
```
**Solutions**:
1. Check application logs: `heroku logs --tail --app your-app-name`
2. Verify `$PORT` environment variable is used in the application
3. Check that the WAR file is built correctly
4. Increase dyno memory if needed: `heroku ps:scale web=1:standard-1x`

**Issue**: Review Apps not being destroyed
**Solutions**:
1. Enable automatic destruction in Heroku Dashboard
2. Manually destroy old apps: `heroku apps:destroy app-name --confirm app-name`
3. Set up automated cleanup with GitHub Actions

##### Debug Commands

```bash
# Check Heroku configuration
heroku config --app your-app-name

# View application processes
heroku ps --app your-app-name

# Access application shell
heroku run bash --app your-app-name

# View build logs
heroku builds --app your-app-name

# Check app size and resources
heroku apps:info your-app-name
```

#### Cost Considerations

- **Free Tier**: Up to 5 apps, 550-1000 dyno hours/month
- **Review Apps**: Each PR creates a new app (counts toward app limit)
- **Automatic Scaling**: Consider destroying stale review apps automatically
- **Production**: Hobby ($7/month) or Professional ($25-500/month) plans for production use

For more information, see [Heroku Pricing](https://www.heroku.com/pricing)

### Custom Preview Environments

To deploy to custom infrastructure, modify the workflow:

```yaml
- name: Deploy to Preview Environment
  if: vars.PREVIEW_DEPLOY_ENABLED == 'true'
  run: |
    # Add your deployment commands here
    # Examples:
    # - Deploy to AWS ECS/Fargate
    # - Deploy to Kubernetes cluster
    # - Deploy to DigitalOcean App Platform
    # - Upload to S3 and update CloudFront
```

## Troubleshooting

### Common Issues

#### Build Failures

**Problem**: Gradle build fails with dependency resolution errors
```
Could not resolve com.android.tools.build:gradle:8.7.1
```

**Solution**: Check network connectivity and Android SDK setup
```bash
# Verify Java version
java -version

# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

#### Deployment Failures

**Problem**: Heroku deployment fails
```
Error: Could not find or access app
```

**Solutions**:
1. Verify `HEROKU_APP_NAME` variable is set correctly
2. Check `HEROKU_API_KEY` secret is valid
3. Ensure app exists in Heroku dashboard

#### Artifact Download Issues

**Problem**: Can't download artifacts from GitHub Actions

**Solutions**:
1. Check artifact retention period (7 days for preview, 30 days for main)
2. Verify you have repository access permissions
3. Check if the workflow run completed successfully

### Getting Help

1. **Check GitHub Actions logs** for detailed error messages
2. **Review workflow run artifacts** for build outputs
3. **Check repository issues** for known problems
4. **Contact repository maintainers** for deployment-specific questions

### Debugging Workflows

Enable debug logging by adding this to your workflow run:

```bash
# Add this environment variable to workflow steps
ACTIONS_STEP_DEBUG: true
```

Or check logs in the GitHub Actions UI for detailed execution information.