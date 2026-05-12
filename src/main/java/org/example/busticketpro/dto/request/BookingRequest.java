package org.example.busticketpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "Chuyến xe không được để trống")
    private Long tripId;

    @NotNull(message = "Ghế không được để trống")
    private Long seatId;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100)
    private String passengerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9}$",
            message = "Số điện thoại không hợp lệ (VD: 0912345678)"
    )
    private String passengerPhone;

    @Email(message = "Email không hợp lệ")
    private String passengerEmail;

    private String paymentMethod;
}