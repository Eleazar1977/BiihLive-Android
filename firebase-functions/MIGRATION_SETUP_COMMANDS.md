# 🚀 Firebase Functions v7 Migration - Environment Variables Setup

## 📋 Prerequisites

1. **Update dependencies** (already done):
   ```json
   "firebase-functions": "^7.0.0"
   ```

2. **Have AWS SES credentials ready**:
   - SMTP Username (AWS Access Key for SES)
   - SMTP Password (AWS Secret Key for SES)
   - SMTP Host (e.g., `email-smtp.eu-west-1.amazonaws.com`)

## 🔧 Step 1: Install/Update Firebase CLI

```bash
# Install or update Firebase CLI
npm install -g firebase-tools

# Login to Firebase (if not already logged in)
firebase login

# Navigate to your project directory
cd C:\Users\asus\AndroidStudioProjects\Biihlive-Android\firebase-functions
```

## ⚙️ Step 2: Configure Environment Parameters

### 🔒 Set Secret Values (SMTP Credentials)

```bash
# Set SMTP username (AWS SES Access Key)
firebase functions:secrets:set SMTP_USER

# Set SMTP password (AWS SES Secret Key)
firebase functions:secrets:set SMTP_PASSWORD
```

**When prompted, enter your AWS SES SMTP credentials:**
- SMTP_USER: Enter your AWS SES SMTP username
- SMTP_PASSWORD: Enter your AWS SES SMTP password

### 📧 Set Public Configuration Values

```bash
# Set SMTP host (default: email-smtp.eu-west-1.amazonaws.com)
firebase functions:config:set smtp.host="email-smtp.eu-west-1.amazonaws.com"

# Set SMTP port (default: 587)
firebase functions:config:set smtp.port="587"

# Set from email address
firebase functions:config:set email.from="noreply@biihlive.com"
```

**Alternative method using Firebase CLI for public values:**

```bash
# For local testing, you can also set these in .env.local
echo "SMTP_HOST=email-smtp.eu-west-1.amazonaws.com" >> .env.local
echo "SMTP_PORT=587" >> .env.local
echo "FROM_EMAIL=noreply@biihlive.com" >> .env.local
```

## 🔄 Step 3: Replace Current Functions

1. **Backup current file:**
   ```bash
   cp password-recovery-functions.js password-recovery-functions-backup.js
   ```

2. **Replace with v7 version:**
   ```bash
   cp password-recovery-functions-v7.js password-recovery-functions.js
   ```

## 📦 Step 4: Install Dependencies

```bash
# Install dependencies for Firebase Functions v7
cd functions
npm install
```

## 🧪 Step 5: Test Locally

```bash
# Start Firebase emulator for testing
firebase emulators:start --only functions

# Test specific function
firebase functions:shell
```

## 🚀 Step 6: Deploy to Production

```bash
# Deploy functions to Firebase
firebase deploy --only functions

# Or deploy specific functions
firebase deploy --only functions:sendPasswordRecoveryCode,verifyPasswordRecoveryCode,resetPasswordWithCode
```

## 🔍 Step 7: Verify Deployment

```bash
# Check function logs
firebase functions:log

# Verify secrets are set
firebase functions:secrets:access SMTP_USER --project biihlive-aa5c3
firebase functions:secrets:access SMTP_PASSWORD --project biihlive-aa5c3
```

## 🛠️ Troubleshooting Commands

### Check Current Configuration:
```bash
# List all secrets
firebase functions:secrets:list

# Check current config
firebase functions:config:get

# Check project info
firebase projects:list
firebase use --list
```

### Update Secrets:
```bash
# Update existing secret
firebase functions:secrets:set SMTP_USER
firebase functions:secrets:set SMTP_PASSWORD
```

### Remove Old Config (if needed):
```bash
# Remove old functions.config() values
firebase functions:config:unset aws_ses
```

## 📋 AWS SES Configuration Values

For your reference, these are the typical AWS SES values you'll need:

| Parameter | Example Value | Description |
|-----------|---------------|-------------|
| SMTP_HOST | `email-smtp.eu-west-1.amazonaws.com` | AWS SES SMTP endpoint |
| SMTP_PORT | `587` | SMTP port (587 for TLS) |
| SMTP_USER | `AKIA...` | AWS Access Key for SES |
| SMTP_PASSWORD | `BPh3...` | AWS Secret Key for SES |
| FROM_EMAIL | `noreply@biihlive.com` | Verified sender email |

## ✅ Validation Steps

After completing the migration:

1. **Test password recovery flow** through your app
2. **Check Firebase Console logs** for any errors
3. **Verify emails are being sent** successfully
4. **Monitor function execution times** and costs

## 🔗 Useful Links

- [Firebase Functions v7 Documentation](https://firebase.google.com/docs/functions)
- [Environment Parameters Guide](https://firebase.google.com/docs/functions/config-env)
- [AWS SES SMTP Setup](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/smtp-credentials.html)

---

## 🚨 Important Notes

1. **Secrets are encrypted** and can only be accessed by your functions
2. **Environment parameters** are region-specific
3. **Test thoroughly** in development before deploying to production
4. **Monitor costs** as v7 may have different pricing
5. **Keep backups** of your working configuration

## 📞 Need Help?

If you encounter issues during migration:
1. Check Firebase Console logs
2. Verify all environment variables are set correctly
3. Test with Firebase emulators first
4. Compare with the original working functions.config() values