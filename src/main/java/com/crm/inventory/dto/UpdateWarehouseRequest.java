package com.crm.inventory.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWarehouseRequest {
    @Size(min = 3, max = 128, message = "name must be between 3 and 128 characters")
    private String name;

    @Size(max = 255, message = "address cannot exceed 255 characters")
    private String address;

    private Boolean isActive;
}
