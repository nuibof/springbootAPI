package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class FavoriteAdminDTO implements Serializable {
    private Long   id;        // product id
    private String name;      // product name
    private String imageUrl;  // product image
    private long   likes;     // total favorites
}
