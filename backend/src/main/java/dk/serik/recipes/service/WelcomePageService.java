package dk.serik.recipes.service;

import dk.serik.recipes.dto.WelcomePageDTO;

public interface WelcomePageService {
    WelcomePageDTO get();

    /** Sets the Welcome page's picture, or none for a null one. */
    WelcomePageDTO update(WelcomePageDTO dto);
}
