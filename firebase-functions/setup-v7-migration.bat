@echo off
echo ========================================
echo Firebase Functions v7 Migration Setup
echo ========================================
echo.

echo 1. Checking Firebase CLI...
firebase --version
if errorlevel 1 (
    echo ERROR: Firebase CLI not found. Please install it first:
    echo npm install -g firebase-tools
    pause
    exit /b 1
)

echo.
echo 2. Setting up secrets (you will be prompted for values)...
echo.
echo Please enter your AWS SES SMTP username when prompted:
firebase functions:secrets:set SMTP_USER --project biihlive-aa5c3

echo.
echo Please enter your AWS SES SMTP password when prompted:
firebase functions:secrets:set SMTP_PASSWORD --project biihlive-aa5c3

echo.
echo 3. Setting up environment parameters...
firebase functions:config:set smtp.host="email-smtp.eu-west-1.amazonaws.com" --project biihlive-aa5c3
firebase functions:config:set smtp.port="587" --project biihlive-aa5c3
firebase functions:config:set email.from="noreply@biihlive.com" --project biihlive-aa5c3

echo.
echo 4. Backing up current functions...
if exist password-recovery-functions.js (
    copy password-recovery-functions.js password-recovery-functions-backup.js
    echo Backup created: password-recovery-functions-backup.js
)

echo.
echo 5. Installing dependencies...
cd functions
npm install firebase-functions@^7.0.0
cd ..

echo.
echo 6. Replacing with v7 version...
copy password-recovery-functions-v7.js password-recovery-functions.js

echo.
echo ========================================
echo Migration setup completed!
echo ========================================
echo.
echo Next steps:
echo 1. Test locally: firebase emulators:start --only functions
echo 2. Deploy: firebase deploy --only functions --project biihlive-aa5c3
echo 3. Verify: firebase functions:log --project biihlive-aa5c3
echo.
pause