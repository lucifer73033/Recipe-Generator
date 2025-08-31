# Smart Recipe Generator - Deployment Guide

## 🚀 Safe Deployment Configuration

This application has been configured for safe deployment and git push by removing all hardcoded sensitive information.

## 🔐 Environment Variables

### Required Variables

Set these environment variables before running the application:

```bash
# MongoDB connection string
export MONGODB_URI="mongodb://localhost:27017/recipe_generator"

# OpenRouter API key for LLM recipe generation
export OPENROUTER_API_KEY="your_openrouter_api_key_here"
```

### Optional Variables

```bash
# Logging levels
export LOG_LEVEL="INFO"                    # DEBUG, INFO, WARN, ERROR
export SECURITY_LOG_LEVEL="WARN"           # DEBUG, INFO, WARN, ERROR

# Database seeding
export SEED_ON_START="false"               # true for dev, false for production

# OpenRouter configuration
export OPENROUTER_BASE_URL="https://openrouter.ai/api/v1"
export OPENROUTER_MODEL="google/gemini-2.5-flash"

# Server configuration
export SERVER_PORT="8080"
```

## 🏃‍♂️ Running the Application

### Development Mode
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production Mode
```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

## 📁 Environment File Setup

Create a `.env` file in your project root (DO NOT commit this file):

```bash
# .env (create this file locally, never commit it)
MONGODB_URI=mongodb://localhost:27017/recipe_generator
OPENROUTER_API_KEY=sk-or-v1-your-actual-key-here
LOG_LEVEL=INFO
SECURITY_LOG_LEVEL=WARN
SEED_ON_START=false
```

## 🚫 What Was Removed for Security

The following sensitive information has been removed from `application.yml`:

- ❌ Google OAuth client ID and secret
- ❌ OpenRouter API key
- ❌ Hardcoded database credentials
- ❌ Development-specific configurations

## ✅ Security Best Practices

1. **Never commit `.env` files** to version control
2. **Use environment variables** for all sensitive configuration
3. **Different profiles** for development and production
4. **Secure defaults** that don't expose sensitive information
5. **Documentation** of all required environment variables

## 🔍 Verification

To verify your configuration is working:

1. Check that the application starts without errors
2. Verify MongoDB connection is successful
3. Test LLM recipe generation (requires valid OpenRouter API key)
4. Check logs for any configuration warnings

## 🆘 Troubleshooting

### Common Issues

1. **MongoDB Connection Failed**
   - Verify `MONGODB_URI` is correct
   - Ensure MongoDB is running

2. **LLM Generation Fails**
   - Check `OPENROUTER_API_KEY` is valid
   - Verify OpenRouter account has credits

3. **Application Won't Start**
   - Check all required environment variables are set
   - Review application logs for specific errors

### Getting Help

- Check the application logs for detailed error messages
- Verify all environment variables are properly set
- Ensure MongoDB and other dependencies are running



