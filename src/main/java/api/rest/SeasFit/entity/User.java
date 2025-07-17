package api.rest.SeasFit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "[user]")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userName;
    private String fullName;
    private String password;
    private Boolean gender;
    private String identityCard;
    private String email;
    private String phone;
    private String dateOfBirth;
    private String address;
    private String imageUrl;
    private String status;
    private String role;
    private LocalDateTime createdAt;
}