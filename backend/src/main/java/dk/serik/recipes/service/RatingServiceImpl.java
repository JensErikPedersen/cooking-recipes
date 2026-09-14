package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.RatingDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.RatingMapper;
import dk.serik.recipes.model.Rating;
import dk.serik.recipes.repository.RatingJpaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        readOnly = false,
        timeout = 5)
@AllArgsConstructor
public class RatingServiceImpl implements RatingService {
    private RatingJpaRepository repository;

    private Session session;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<RatingDTO> findById(String id) {
        Optional<Rating> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.RATING_ID_IS_NULL, "Rating"));
        if(optional.isPresent()) {
            return Optional.of(RatingMapper.from(optional.get()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<RatingDTO> findAll() {
        return repository.findAll().stream()
                .map(RatingMapper::from)
                .collect(Collectors.toList());
    }

    @Override
    public RatingDTO save(RatingDTO dto) {
        if(Objects.nonNull(dto)) {
            Rating entity = new Rating();
            entity.setRating(dto.getRating());
            entity.setDescription(dto.getDescription());
            entity.setCreatedBy(session.getUserName());
            Rating savedRating = repository.save(entity);
            return RatingMapper.from(savedRating);
        } else {
            throw ServiceException.builder()
                    .code(ApplicationErrorCodes.RATING_DTO_IS_NULL.getCode())
                    .message("Rating is null")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

    }

    @Override
    public boolean delete(String id) {
        Optional<Rating> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.RATING_ID_IS_NULL, "Rating"));
        if(optional.isPresent()) {
            repository.delete(optional.get());
            return true;
        }
        return false;
    }

    @Override
    public RatingDTO update(RatingDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.RATING_ID_IS_NULL, "Rating Id is required for an update");
        }
        Optional<Rating> optional = repository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.RATING_ID_IS_NULL, "Rating"));
        if(optional.isPresent()) {
            Rating managedRating = optional.get();
            managedRating.setRating(dto.getRating());
            managedRating.setDescription(dto.getDescription());
            Rating savedRating = repository.save(managedRating);
            return RatingMapper.from(savedRating);
        } else {
            throw ServiceException.builder()
                    .message("Could not update Rating with id " + dto.getId() + " since it was not found")
                    .code(ApplicationErrorCodes.RATING_NOT_FOUND.getCode())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }
    }
}
