package api.rest.SeasFit.dto;

import api.rest.SeasFit.entity.ProductVariant;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantOnlyDTO {
    private Long id;
    private int quantity;
    private BigDecimal price;
    private String colorName;
    private String colorHex;
    private String sizeLabel;

    public VariantOnlyDTO(ProductVariant v) {
        this.id = v.getId();
        this.quantity = v.getQuantity();
        this.price = v.getPrice();
        this.colorName = v.getColor().getName();
        this.colorHex = v.getColor().getHexCode();
        this.sizeLabel = v.getSize().getLabel();
    }
}
