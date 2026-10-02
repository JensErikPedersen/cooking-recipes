package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.TagMapper;
import dk.serik.recipes.model.Tag;
import dk.serik.recipes.repository.RecipeJpaRepository;
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

    private RecipeJpaRepository recipeRepository;

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
        rejectDuplicateName(dto.getName(), null);
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
            rejectDeleteInUse(optional.get());
            repository.delete(optional.get());
            return true;
        }
        return false;
    }

    // As in CategoryServiceImpl: the unique index on name and the recipe_tag foreign key refuse
    // these anyway, at commit and without a reason; checking first gives one.
    private void rejectDuplicateName(String name, UUID ownId) {
        repository.findTagByName(name)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw ServiceException.builder()
                            .message(String.format("A tag named '%s' already exists", name))
                            .code(ApplicationErrorCodes.TAG_ALREADY_EXISTS.getCode())
                            .httpStatus(HttpStatus.CONFLICT)
                            .field("name")
                            .build();
                });
    }

    private void rejectDeleteInUse(Tag tag) {
        long recipes = recipeRepository.countByTagsId(tag.getId());
        if (recipes > 0) {
            throw ServiceException.builder()
                    .message(String.format("Tag '%s' is used by %d %s and cannot be deleted",
                            tag.getName(), recipes, recipes == 1 ? "recipe" : "recipes"))
                    .code(ApplicationErrorCodes.TAG_IN_USE.getCode())
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }
    }

    @Override
    public TagDTO update(TagDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag Id is required for an update");
        }
        Optional<Tag> optional = repository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag"));
        if(optional.isPresent()) {
            Tag managedTag = optional.get();
            rejectDuplicateName(dto.getName(), managedTag.getId());
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
