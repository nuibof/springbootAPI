package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Banner;
import api.rest.SeasFit.repository.BannerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public Banner getBanner() {
        return entityManager.createQuery("SELECT b FROM Banner b", Banner.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    public Banner saveBanner(Banner banner) {
        return bannerRepository.save(banner);
    }
}
