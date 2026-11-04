package com.crm.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @NotBlank(message = "code is required")
    @Size(min = 2, max = 32, message = "code must be between 2 and 32 characters")
    private String code;

    @NotBlank(message = "name is required")
    @Size(min = 3, max = 128, message = "name must be between 3 and 128 characters")
    private String name;

    @Size(max = 255, message = "address cannot exceed 255 characters")
    private String address;
}
