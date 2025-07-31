package api.rest.SeasFit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "big_text", columnDefinition = "NVARCHAR(100)")
    private String bigText;

    @Column(name = "sub_text", columnDefinition = "NVARCHAR(100)")
    private String subText;

    @Column(name = "button_text", columnDefinition = "NVARCHAR(50)")
    private String buttonText;
}
