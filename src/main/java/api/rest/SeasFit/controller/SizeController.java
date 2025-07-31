package api.rest.SeasFit.controller;

import api.rest.SeasFit.service.SizeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sizes")
public class SizeController {
    final private SizeService sizeService;
    public SizeController(SizeService sizeService) {
        this.sizeService = sizeService;
    }
    @GetMapping("")
    public Object getAllSizes() {
        return sizeService.getAllSizes();
    }
}
