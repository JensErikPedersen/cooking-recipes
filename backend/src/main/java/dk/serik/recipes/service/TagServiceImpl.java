package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.TagMapper;
import dk.serik.recipes.model.Tag;
import dk.serik.recipes.repository.TagJpaRepository;
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
@AllArgsConstructor
@Slf4j
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        readOnly = false,
        timeout = 5)
public class TagServiceImpl implements TagService {

    private TagJpaRepository repository;

    private Session session;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<TagDTO> findById(String id) {
        Optional<Tag> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag"));
        if (optional.isPresent()) {
            return Optional.ofNullable(TagMapper.from(optional.get()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<TagDTO> findAll() {
        return repository.findAll().stream()
                .map(TagMapper::from)
                .collect(Collectors.toList());
    }

    @Override
    public TagDTO save(TagDTO dto) {
        if(Objects.isNull(dto)) {
            throw ServiceException.badRequest(ApplicationErrorCodes.TAG_DTO_IS_NULL, "TagDTO is null");
        }
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setCreatedBy(session.getUserName());
        Tag savedTag = repository.save(tag);
        return TagMapper.from(savedTag);
    }

    @Override
    public boolean delete(String id) {
        Optional<Tag> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag"));
        if(optional.isPresent()) {
            repository.delete(optional.get());
            return true;
        }
        return false;
    }

    @Override
    public TagDTO update(TagDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag Id is required for an update");
        }
        Optional<Tag> optional = repository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag"));
        if(optional.isPresent()) {
            Tag managedTag = optional.get();
            managedTag.setName(dto.getName());
            Tag savedTag = repository.save(managedTag);
            return TagMapper.from(savedTag);
        }else {
            throw ServiceException.builder()
                    .message("Could not update Tag with id " + dto.getId() + " since it was not found")
                    .code(ApplicationErrorCodes.TAG_NOT_FOUND.getCode())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<TagDTO> findTagByName(String name) {
        Optional<Tag> optional = repository.findTagByName(name);
        if(optional.isPresent()) {
            return Optional.of(TagMapper.from(optional.get()));
        }

        return Optional.empty();
    }
}
