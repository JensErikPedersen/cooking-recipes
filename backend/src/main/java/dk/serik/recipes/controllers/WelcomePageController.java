package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.WelcomePageDTO;
import dk.serik.recipes.service.WelcomePageService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Welcome page's picture (docs/PLAN_V8.md decision 2): one resource, read and replaced, never
 * created or deleted.
 */
@RestController
@RequestMapping(WelcomePageController.PATH)
@AllArgsConstructor
public class WelcomePageController {

    static final String PATH = "/api/v1/welcome-page";

    private final WelcomePageService welcomePageService;

    @GetMapping
    public WelcomePageDTO get() {
        return welcomePageService.get();
    }

    @PutMapping
    public WelcomePageDTO update(@RequestBody WelcomePageDTO dto) {
        return welcomePageService.update(dto);
    }
}
