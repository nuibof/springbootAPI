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

    // Ảnh + text chính
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "big_text", columnDefinition = "NVARCHAR(100)")
    private String bigText;

    @Column(name = "sub_text", columnDefinition = "NVARCHAR(100)")
    private String subText;

    @Column(name = "button_text", columnDefinition = "NVARCHAR(50)")
    private String buttonText;

    // Ảnh phụ 1..3
    @Column(name = "sub_image_url_1", length = 255)
    private String subImageUrl1;

    @Column(name = "sub_image_url_2", length = 255)
    private String subImageUrl2;

    @Column(name = "sub_image_url_3", length = 255)
    private String subImageUrl3;

    // Text phụ 1..3
    @Column(name = "sub_text_1", columnDefinition = "NVARCHAR(100)")
    private String subText1;

    @Column(name = "sub_text_2", columnDefinition = "NVARCHAR(100)")
    private String subText2;

    @Column(name = "sub_text_3", columnDefinition = "NVARCHAR(100)")
    private String subText3;

    // Button label phụ 1..3
    @Column(name = "sub_button_text_1", columnDefinition = "NVARCHAR(50)")
    private String subButtonText1;

    @Column(name = "sub_button_text_2", columnDefinition = "NVARCHAR(50)")
    private String subButtonText2;

    @Column(name = "sub_button_text_3", columnDefinition = "NVARCHAR(50)")
    private String subButtonText3;

    // Button URL chính
    @Column(name = "button_url", length = 255)
    private String buttonUrl;

    // Button URL phụ 1..3
    @Column(name = "sub_button_url_1", length = 255)
    private String subButtonUrl1;

    @Column(name = "sub_button_url_2", length = 255)
    private String subButtonUrl2;

    @Column(name = "sub_button_url_3", length = 255)
    private String subButtonUrl3;
}
