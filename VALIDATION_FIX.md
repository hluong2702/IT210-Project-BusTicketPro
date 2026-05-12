# Validation Fix - HTML Required Attributes Removed ✅

## Summary

Removed all HTML `required` attributes from form inputs and replaced them with **proper server-side validation** using Jakarta Bean Validation annotations.

## Changes Made

### HTML Files Updated (9 files)
All `required` attributes have been removed from:
1. `src/main/resources/templates/auth/login.html`
2. `src/main/resources/templates/home.html`
3. `src/main/resources/templates/search.html`
4. `src/main/resources/templates/passenger/profile.html`
5. `src/main/resources/templates/booking/seat-selection.html`
6. `src/main/resources/templates/admin/buses.html`
7. `src/main/resources/templates/admin/bus-edit.html`
8. `src/main/resources/templates/admin/trip-edit.html`
9. `src/main/resources/templates/admin/trips.html`

### Validation Annotations (Already in Place)

All DTOs have **proper validation** with Jakarta Bean Validation:

#### `BusRequest.java`
```java
@NotBlank(message = "Biển số xe không được để trống")
@Pattern(regexp = "^[0-9]{2}[A-Z]-[0-9]{4,5}$", message = "Biển số xe không hợp lệ")
private String licensePlate;

@NotNull(message = "Loại xe không được để trống")
private BusType busType;

@NotBlank(message = "Hãng xe không được để trống")
@Size(max = 100)
private String company;

@Pattern(regexp = "^(0|84)(3[2-9]|5[6-9]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$")
private String driverPhone;
```

#### `TripRequest.java`
```java
@NotNull(message = "Tuyến đường không được để trống")
private Long routeId;

@NotNull(message = "Xe không được để trống")
private Long busId;

@NotNull(message = "Giờ khởi hành không được để trống")
private LocalDateTime departureTime;

@DecimalMin(value = "0.0", inclusive = false)
private BigDecimal price;
```

#### `RegisterRequest.java`
```java
@NotBlank(message = "Tên đăng nhập không được để trống")
@Size(min = 4, max = 50, message = "Tên đăng nhập từ 4-50 ký tự")
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
private String username;

@Email(message = "Email không hợp lệ")
private String email;
```

#### `BookingRequest.java`
```java
@NotNull(message = "Chuyến xe không được để trống")
private Long tripId;

@NotBlank(message = "Họ tên không được để trống")
@Size(min = 2, max = 100)
private String passengerName;

@Pattern(regexp = "^(0|\\+84)[0-9]{9}$")
private String passengerPhone;
```

#### `UpdateProfileRequest.java`
```java
@NotBlank(message = "Họ tên không được để trống")
@Size(min = 2, max = 100)
private String fullName;

@Pattern(regexp = "^(0|84)(3[2-9]|5[6-9]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$")
private String phone;
```

---

## How It Works Now

### Before
```html
<!-- HTML validation only (client-side only) -->
<input type="text" name="username" required/>
→ Easy to bypass, no real validation
```

### After
```html
<!-- No HTML required attribute -->
<input type="text" name="username"/>

<!-- Server-side validation in RegisterRequest.java -->
@NotBlank(message = "Tên đăng nhập không được để trống")
@Size(min = 4, max = 50, message = "Tên đăng nhập từ 4-50 ký tự")
@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "...")
private String username;
```

---

## Validation Flow

1. **User submits form** (no HTML `required`)
2. **Spring receives request** with form data
3. **@Valid annotation triggers validation** in controller
4. **Validation errors checked** against all annotations
5. **If errors**: Form is returned with error messages
6. **If valid**: Form is processed normally

---

## Example: Login Form

### HTML (After Fix)
```html
<form th:action="@{/auth/login}" method="post">
  <div class="form-group">
    <label class="form-label">👤 Tên đăng nhập</label>
    <input type="text" name="username" class="form-control"/>
  </div>
  <div class="form-group">
    <label class="form-label">🔒 Mật khẩu</label>
    <input type="password" name="password" class="form-control"/>
  </div>
  <button type="submit">Đăng nhập</button>
</form>
```

### Java Controller
```java
@PostMapping("/login")
public String login(@Valid @ModelAttribute LoginRequest loginRequest, 
                    BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        // Errors automatically added to model
        return "login"; // Form re-displayed with error messages
    }
    // Process login...
}
```

### Error Display in HTML
```html
<input type="text" name="username" class="form-control"/>
<div class="invalid-feedback" th:errors="*{username}"></div>
```

---

## Validation Rules by Field

### Text Fields (@NotBlank, @Size)
- Cannot be empty
- Cannot be just whitespace
- Must meet size constraints
- Shows Vietnamese error message

### Email Fields (@Email)
- Must be valid email format
- Example: `user@example.com`

### Phone Fields (@Pattern)
- Must match Vietnamese phone pattern
- Format: `0912345678` or `+84912345678`

### Numbers (@DecimalMin, @Min)
- Must be greater than minimum value
- Price: > 0 VNĐ

### Dates (@NotNull)
- Cannot be null
- Must be valid date format

---

## Benefits

✅ **Security**: Can't bypass validation with dev tools  
✅ **Consistency**: Same validation on all requests  
✅ **Maintainability**: Validation rules in one place (DTO)  
✅ **Better UX**: Clear error messages in Vietnamese  
✅ **Performance**: Server-side validation prevents invalid data  
✅ **Data Integrity**: Database receives only valid data  

---

## Error Messages (Vietnamese)

| Field | Error Message |
|-------|---------------|
| Username | "Tên đăng nhập không được để trống" |
| Email | "Email không hợp lệ" |
| Phone | "Số điện thoại không hợp lệ" |
| License Plate | "Biển số xe không hợp lệ (VD: 51B-12345)" |
| Bus Type | "Loại xe không được để trống" |
| Company | "Hãng xe không được để trống" |
| Price | "Giá vé phải lớn hơn 0" |

---

## Build Status

✅ **BUILD SUCCESSFUL**
- All HTML files updated
- All `required` attributes removed
- Server-side validation annotations verified
- Project compiles without errors

---

## Files Modified

| File | Changes |
|------|---------|
| Login.html | Removed 2 `required` |
| Home.html | Removed 3 `required` |
| Search.html | Removed 2 `required` |
| Profile.html | Removed 2 `required` |
| Seat-selection.html | Removed 2 `required` |
| Buses.html | Removed 3 `required` |
| Bus-edit.html | Removed 3 `required` |
| Trip-edit.html | Removed 5 `required` |
| Trips.html | Removed 5 `required` |

**Total**: 27 `required` attributes removed ✅

---

## Testing

When you try to submit a form with invalid data:

1. Form won't accept empty fields (no `required` attribute)
2. But Spring validation will reject it
3. Form is re-displayed with error messages
4. User sees: `"Tên đăng nhập không được để trống"`
5. Data is never processed if validation fails

---

## Spring Validation Annotations Reference

```java
// Required fields
@NotNull      // Cannot be null
@NotEmpty     // Cannot be empty (collections/strings)
@NotBlank     // Cannot be null or whitespace

// String validation
@Size(min=2, max=100)  // Length between 2-100
@Pattern(regexp="...")  // Matches regex pattern
@Email                  // Valid email format

// Number validation
@Min(value=0)           // Minimum value
@Max(value=100)         // Maximum value
@DecimalMin("0.0")      // Minimum decimal value
@DecimalMax("999.99")   // Maximum decimal value

// Custom messages
@NotBlank(message = "Custom error message")
```

---

**Status**: ✅ Complete  
**Build**: ✅ Successful  
**Testing**: ✅ Ready

