package api.rest.SeasFit.dto;

public class BestsellerDTO {
    private Long id;
    private String name;
    private Long totalSold;

    public BestsellerDTO(Long id, String name, Long totalSold) {
        this.id = id; this.name = name; this.totalSold = totalSold;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getTotalSold() { return totalSold; }
}
