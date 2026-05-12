# Quick Email Setup - macOS

## Option 1: Quick Setup (Using Gmail)

### Step 1: Get Gmail App Password
1. Visit: https://myaccount.google.com/apppasswords
2. Select `Mail` and `Mac`
3. Copy the 16-character password (e.g., `xxxx xxxx xxxx xxxx`)

### Step 2: Set Environment Variables
```bash
# In terminal:
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=xxxxxxxxxxxx
```

### Step 3: Restart Application
```bash
./gradlew bootRun
```

### Verify It Works
- Create a booking
- Check logs for: `Booking confirmation email sent to customer@email.com`

---

## Option 2: Permanent Setup (Add to ~/.zshrc)
```bash
# Open editor
nano ~/.zshrc

# Add these lines at the end:
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=xxxxxxxxxxxx

# Save: Ctrl+O, Enter, Ctrl+X

# Apply changes:
source ~/.zshrc
```

---

## If Email Still Fails

Check logs for one of these errors:

### "Authentication failed"
→ Wrong password or Gmail app password not generated
→ Solution: Get new app password from https://myaccount.google.com/apppasswords

### "Socket timeout" 
→ Firewall or internet issue
→ Solution: Check if you can ping `smtp.gmail.com`

### "Email service not configured"
→ Environment variables not set correctly
→ Solution: Run `echo $MAIL_USERNAME` to verify it's set

---

## Current Status
- ✅ Email functionality is graceful - app won't crash without email config
- ✅ Logs clearly show if email is disabled
- ⚠️ Email is currently DISABLED (no credentials set)

See full setup guide: `EMAIL_SETUP.md`

