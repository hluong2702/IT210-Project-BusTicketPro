# Email Service - Behavior Reference

## Log Output Examples

### 1. When Email is NOT Configured (Current State)

```
2026-05-12T11:43:32.243+07:00  INFO  o.e.b.service.impl.TicketServiceImpl     : Ticket created: BTP10121420CF9 for seat A01
2026-05-12T11:43:32.244+07:00  WARN  o.e.b.s.i.EmailServiceImpl               : Email service not configured. Booking email for ticket BTP10121420CF9 would have been sent to customer@example.com
2026-05-12T11:43:35.192+07:00  INFO  o.e.b.controller.BookingController      : Booking successful - user can proceed
```

✅ Application continues normally  
✅ No errors or crashes  
✅ Clear warning message  
⚠️ Emails not sent (expected)

---

### 2. When Email IS Configured (After Setting Env Variables)

```
2026-05-12T11:43:32.243+07:00  INFO  o.e.b.service.impl.TicketServiceImpl     : Ticket created: BTP10121420CF9 for seat A01
2026-05-12T11:43:32.244+07:00  INFO  o.e.b.s.i.EmailServiceImpl               : Booking confirmation email sent to customer@example.com
2026-05-12T11:43:35.192+07:00  INFO  o.e.b.controller.BookingController      : Booking successful
```

✅ Ticket created  
✅ Email sent  
✅ User receives confirmation  

---

### 3. If Configuration is Wrong (After Setup)

**Scenario A: Wrong Password**
```
2026-05-12T11:43:32.244+07:00  ERROR o.e.b.s.i.EmailServiceImpl               : Failed to send booking email for ticket BTP10121420CF9: Mail server error - Authentication failed
2026-05-12T11:43:32.245+07:00  INFO  o.e.b.controller.BookingController      : Booking successful despite email error
```

**Scenario B: SMTP Server Unreachable**
```
2026-05-12T11:43:32.244+07:00  ERROR o.e.b.s.i.EmailServiceImpl               : Failed to send booking email for ticket BTP10121420CF9: Mail server error - java.net.ConnectException: Connection refused
2026-05-12T11:43:32.245+07:00  INFO  o.e.b.controller.BookingController      : Booking successful despite email error
```

**Scenario C: Invalid Email Format**
```
2026-05-12T11:43:32.244+07:00  ERROR o.e.b.s.i.EmailServiceImpl               : Failed to send booking email for ticket BTP10121420CF9: Messaging error - Invalid email address
2026-05-12T11:43:32.245+07:00  INFO  o.e.b.controller.BookingController      : Booking successful despite email error
```

✅ Application always continues  
✅ Errors are logged but non-blocking  
✅ Users can still book tickets  

---

## Checking Email Configuration

### From Terminal:
```bash
# Check if variables are set:
echo $MAIL_USERNAME    # Should show: your-email@gmail.com
echo $MAIL_PASSWORD    # Should show: xxxxxxxxxxxx

# If empty, they're not set
```

### From Application Logs:
```bash
# Look at startup logs:
./gradlew bootRun 2>&1 | grep -i mail
```

---

## When Each Email Type is Sent

1. **Booking Confirmation**
   - Sent immediately after ticket creation
   - Contains: ticket code, route, seat, time, price
   - Payment deadline: 30 minutes

2. **Payment Confirmation**  
   - Sent after successful VNPay payment verification
   - Contains: success message, ticket details

3. **Cancellation Notice**
   - Sent when ticket is cancelled (by user or staff)
   - Contains: cancellation reason, ticket details

---

## Email Content

### Booking Email Example:
```
🚌 BusTicketPro - Xác nhận đặt vé

Kính gửi [Passenger Name],

Vé của bạn đã được đặt thành công. Vui lòng thanh toán trong vòng 30 phút.

Thông tin vé:
- Mã vé: BTP10121420CF9
- Tuyến: Hà Nội → TP. Hồ Chí Minh
- Giờ khởi hành: 14:30 12/05/2026
- Ghế: A01
- Xe: 51B-12345 - Limousine
- Tổng tiền: 450,000 đ

Hotline: 1900 1234
```

### Payment Email Example:
```
✅ BusTicketPro - Thanh toán thành công!

Kính gửi [Passenger Name],

Vé BTP10121420CF9 đã được xác nhận thanh toán.

Chúc bạn có chuyến đi vui vẻ! 🎉
```

---

## Troubleshooting Quick Reference

| Error | Solution |
|-------|----------|
| "Email service not configured" | Set MAIL_USERNAME and MAIL_PASSWORD env vars |
| "Authentication failed" | Check Gmail app password (not regular password) |
| "Socket timeout" | Check internet/firewall, or use different SMTP |
| "Invalid email address" | Ensure passenger email is valid in database |
| "Connection refused" | SMTP server unreachable, check host/port |

---

## Performance Impact

- Email sending is **async** (@Async annotation)
- Doesn't block booking process
- User gets instant response
- Emails sent in background
- If email fails, it doesn't affect user experience

---

**Last Updated**: 2026-05-12

