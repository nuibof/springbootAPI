package api.rest.SeasFit.dto;

public class FavoriteDTO {
    private Long id;
    private String name;
    private Long totalFavorites;

    public FavoriteDTO(Long id, String name, Long totalFavorites) {
        this.id = id;
        this.name = name;
        this.totalFavorites = totalFavorites;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getTotalFavorites() { return totalFavorites; }
}
