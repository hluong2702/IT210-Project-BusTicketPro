package org.example.busticketpro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.busticketpro.enums.BusType;

@Data
public class BusRequest {

    @NotBlank(message = "Biển số xe không được để trống")
    @Pattern(regexp = "^[0-9]{2}[A-Z]-[0-9]{4,5}$", message = "Biển số xe không hợp lệ (VD: 51B-12345)")
    private String licensePlate;

    @NotNull(message = "Loại xe không được để trống")
    private BusType busType;

    @NotBlank(message = "Hãng xe không được để trống")
    @Size(max = 100)
    private String company;

    @Size(max = 100)
    private String driverName;

    @Pattern(regexp = "^(0|84)(3[2-9]|5[6-9]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$",
             message = "Số điện thoại không hợp lệ")
    private String driverPhone;

    @Size(max = 20)
    private String color;

    @Size(max = 100)
    private String amenities;
}
