package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.Color;
import api.rest.SeasFit.service.ColorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
public class ColorController {

    private final ColorService colorService;
    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

     @GetMapping
     public List<Color> getAllColors() {
         return colorService.getAllColors();
     }
}
