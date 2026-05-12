# Email Configuration Guide for BusTicketPro

## Overview
The application sends booking confirmations, payment confirmations, and cancellation notices via email. By default, email functionality is **disabled** until you properly configure it.

## Setup Instructions

### For Gmail

#### Step 1: Create a Gmail Account (if you don't have one)
- Go to https://mail.google.com
- Create a new account or use an existing one

#### Step 2: Generate App-Specific Password
1. Go to your Google Account: https://myaccount.google.com
2. Navigate to **Security** (left sidebar)
3. Enable **2-Step Verification** if not already enabled:
   - Click "2-Step Verification"
   - Follow the on-screen instructions
4. After 2FA is enabled, you'll see "App passwords" option
5. Click **App passwords**
6. Select `Mail` and `Windows Computer` (or your device)
7. Google will generate a 16-character password like: `xxxx xxxx xxxx xxxx`
8. Copy this password (without spaces)

#### Step 3: Set Environment Variables

**On macOS/Linux:**
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=xxxxxxxxxxxx  # 16-char app password without spaces
```

**On Windows (PowerShell):**
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="xxxxxxxxxxxx"
```

**Permanent Setup (macOS/Linux):**
Add to your `~/.zshrc` or `~/.bash_profile`:
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=xxxxxxxxxxxx
```

Then run:
```bash
source ~/.zshrc
```

#### Step 4: Restart the Application
```bash
# Kill the running instance (Ctrl+C)
# Then restart with:
./gradlew bootRun
```

### For Other Email Providers (Outlook, Yahoo, etc.)

Update `application.properties`:
```properties
spring.mail.host=smtp.outlook.com           # or smtp.yahoo.com, etc.
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
```

Then set your environment variables accordingly.

## Verification

1. Create a booking in the application
2. Check your email for a booking confirmation
3. Check the application logs:
   ```
   INFO  o.e.b.s.i.EmailServiceImpl : Booking confirmation email sent to customer@example.com
   ```

## Troubleshooting

### Error: "Authentication failed"
- **Cause**: Invalid credentials or app password
- **Solution**: Verify you're using the 16-character app password (not your Gmail password)

### Error: "Socket timeout"
- **Cause**: SMTP server connection timeout
- **Solution**: Check your internet connection or firewall settings

### No error in logs but email not received
- **Cause**: Email configured but email service is not enabled
- **Solution**: Restart the application after setting environment variables

### Emails not sending in production
- **For AWS / Heroku / other cloud platforms:**
  1. Set environment variables in your platform's configuration
  2. Use the platform's SMTP service if available (SES, SendGrid, etc.)
  3. For SendGrid or Mailgun, update `spring.mail.host` and `spring.mail.port` accordingly

## Security Notes

⚠️ **Important:**
- Never commit your email password to version control
- Use app-specific passwords, not your actual email password
- For production, consider using services like:
  - AWS SES (Simple Email Service)
  - SendGrid
  - Mailgun
  - Firebase Email

## Email Features

The application sends three types of emails:

1. **Booking Confirmation** (sent when ticket is created)
   - Ticket number, route, time, seat, bus info, total price
   - Payment deadline reminder

2. **Payment Confirmation** (sent when payment is verified)
   - Confirmation of successful payment
   - Trip details

3. **Cancellation Notice** (sent when ticket is cancelled)
   - Cancellation reason (if provided)
   - Ticket details

## Support

For issues with email configuration:
1. Check the application logs for detailed error messages
2. Verify your credentials using a simple telnet/SMTP test
3. Check email provider's SMTP settings documentation
4. Look for specific error messages in the logs (e.g., "TLS required", "Port incorrect")

---
**Last Updated**: 2026-05-12

