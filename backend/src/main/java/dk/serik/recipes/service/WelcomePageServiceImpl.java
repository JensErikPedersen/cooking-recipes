package dk.serik.recipes.service;

import dk.serik.recipes.dto.WelcomePageDTO;
import dk.serik.recipes.mapper.PictureMapper;
import dk.serik.recipes.model.WelcomePage;
import dk.serik.recipes.repository.PictureJpaRepository;
import dk.serik.recipes.repository.WelcomePageJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        readOnly = false,
        timeout = 5)
public class WelcomePageServiceImpl implements WelcomePageService {

    private WelcomePageJpaRepository repository;

    private PictureJpaRepository pictureRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public WelcomePageDTO get() {
        return from(row());
    }

    @Override
    public WelcomePageDTO update(WelcomePageDTO dto) {
        WelcomePage welcomePage = row();
        welcomePage.setPicture(ServiceArguments.toPicture(dto.picture(), pictureRepository));
        return from(repository.save(welcomePage));
    }

    // The table's one row, created by db.changelog_1.8.xml.
    private WelcomePage row() {
        return repository.findAll().getFirst();
    }

    private static WelcomePageDTO from(WelcomePage welcomePage) {
        return new WelcomePageDTO(PictureMapper.from(welcomePage.getPicture()));
    }
}
