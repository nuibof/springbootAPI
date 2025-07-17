package api.rest.SeasFit.dto;

import lombok.Data;

@Data
public class QuantityDTO {
    private int id; // productVariantId
    private int quantity; // new quantity to set
}
