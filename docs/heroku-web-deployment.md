# Heroku Web Deployment Guide

This guide provides step-by-step instructions for deploying the GDHCN Validator web application to Heroku using the web interface (no CLI required).

## Prerequisites

Before starting, ensure you have:
- A GitHub account with access to this repository
- A Heroku account (free tier is sufficient for testing)
- Repository contains the required files:
  - `Procfile` (already present)
  - `system.properties` (already present)
  - Web module with Spring Boot application

## Step-by-Step Deployment

### 1. Create Heroku Account

1. Visit [https://signup.heroku.com/](https://signup.heroku.com/)
2. Fill in your details and create a free account
3. Verify your email address
4. Log in to your Heroku account

### 2. Create New Heroku App

1. After logging in, you'll see the Heroku Dashboard
2. Click the **"New"** button in the top-right corner
3. Select **"Create new app"** from the dropdown
4. Choose an app name (must be unique across all Heroku apps)
   - Example: `gdhcn-validator-[your-name]` or `who-validator-demo`
   - App name can only contain lowercase letters, numbers, and dashes
5. Select a region (choose the one closest to your users)
   - **United States** or **Europe**
6. Click **"Create app"**

### 3. Configure GitHub Integration

1. In your new app's dashboard, navigate to the **"Deploy"** tab
2. In the **"Deployment method"** section, select **"GitHub"**
3. If this is your first time, you'll need to connect your GitHub account:
   - Click **"Connect to GitHub"**
   - Authorize Heroku to access your GitHub account
   - You may be redirected to GitHub to confirm permissions

### 4. Connect Repository

1. In the **"Connect to GitHub"** section:
   - Search for repository name: `gdhcn-validator`
   - Make sure you're searching in the correct organization/account
2. Find your repository in the search results
3. Click **"Connect"** next to the repository name

### 5. Configure Deployment Settings

#### Option A: Automatic Deploys (Recommended)
1. Scroll down to the **"Automatic deploys"** section
2. Select the branch you want to deploy from (typically `main` or `master`)
3. Optional: Check **"Wait for CI to pass before deploy"** if you have GitHub Actions/CI setup
4. Click **"Enable Automatic Deploys"**

#### Option B: Manual Deploys
1. Scroll down to the **"Manual deploy"** section
2. Select the branch you want to deploy
3. Click **"Deploy Branch"**

### 6. Monitor Initial Deployment

1. After triggering a deployment, you'll see the build logs in real-time
2. The build process will:
   - Download your code from GitHub
   - Detect it's a Java application
   - Install Java 17 (as specified in `system.properties`)
   - Run `./gradlew build` to build the web module
   - Start the application using the `Procfile` command

3. Look for these success indicators:
   ```
   -----> Java app detected
   -----> Installing OpenJDK 17
   -----> Executing ./gradlew build
   -----> Discovering process types
          Procfile declares types -> web
   -----> Compressing...
   -----> Launching...
   ```

### 7. Access Your Deployed Application

1. Once deployment is successful, click **"View"** button or
2. Visit `https://[your-app-name].herokuapp.com`
3. You should see the GDHCN Validator web interface

### 8. Configure Environment Variables (If Needed)

If your application requires environment variables:

1. Go to the **"Settings"** tab in your Heroku app dashboard
2. Scroll down to **"Config Vars"** section
3. Click **"Reveal Config Vars"**
4. Add any required environment variables:
   - Click **"Add"**
   - Enter **KEY** and **VALUE**
   - Click **"Add"** to save
5. Common variables might include:
   - Database URLs
   - API keys
   - Trust registry configurations

### 9. View Application Logs

To troubleshoot issues:

1. In your app dashboard, click **"More"** in the top-right
2. Select **"View logs"**
3. Or go to the **"Activity"** tab to see deployment history

## Common Issues and Solutions

### Build Failures

**Problem**: Gradle build fails during deployment
**Solution**: 
- Check that `gradlew` has execute permissions in your repository
- Ensure all dependencies are properly specified in `build.gradle`
- Verify Java version in `system.properties` matches your project requirements

**Problem**: "Application error" when accessing the app
**Solution**:
- Check the logs for error messages
- Verify the `Procfile` is correctly configured
- Ensure your web module builds successfully locally

### Memory Issues

**Problem**: Application crashes due to memory limits
**Solution**:
- Add `JAVA_OPTS` config variable with memory settings:
  - Key: `JAVA_OPTS`
  - Value: `-Xmx512m -Xms256m`

### Port Configuration

**Problem**: Application doesn't start due to port issues
**Solution**:
- Ensure your Spring Boot application uses `$PORT` environment variable
- This should be automatic with Spring Boot on Heroku

## Updating Your Deployment

### Automatic Updates (if enabled)
- Simply push changes to your connected GitHub branch
- Heroku will automatically detect changes and redeploy

### Manual Updates
1. Go to **"Deploy"** tab
2. Scroll to **"Manual deploy"** section
3. Select the branch with your changes
4. Click **"Deploy Branch"**

## Managing Your App

### Scaling
1. Go to **"Resources"** tab
2. Adjust dyno types and quantities
3. Free tier includes 550-1000 free dyno hours per month

### Custom Domain (Optional)
1. Go to **"Settings"** tab
2. Scroll to **"Domains"** section
3. Click **"Add domain"** to use a custom domain

### SSL/HTTPS
- Heroku provides automatic SSL for all `*.herokuapp.com` domains
- Custom domains require SSL certificates (available in paid plans)

## Security Considerations

1. **Environment Variables**: Never commit sensitive data to your repository
2. **Dependencies**: Keep dependencies updated
3. **Access Control**: Use Heroku's team features to manage access
4. **Monitoring**: Set up log monitoring for production applications

## Support and Troubleshooting

- **Heroku Documentation**: [https://devcenter.heroku.com/](https://devcenter.heroku.com/)
- **Application Logs**: Available in Heroku dashboard under "More" → "View logs"
- **GitHub Integration**: [https://devcenter.heroku.com/articles/github-integration](https://devcenter.heroku.com/articles/github-integration)

## Cost Information

- **Free Tier**: 550-1000 free dyno hours per month
- **Hobby Tier**: $7/month for always-on apps
- **Production Plans**: Start at $25/month with enhanced features

The free tier is perfect for development and testing, while production deployments should consider paid plans for better performance and uptime guarantees.