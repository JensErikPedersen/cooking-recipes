package dk.serik.recipes.service;

import dk.serik.recipes.dto.UnitDTO;

import java.util.List;
import java.util.Optional;

public interface UnitService {
    Optional<UnitDTO> findById(String id);
    List<UnitDTO> findAll();
    UnitDTO save(UnitDTO dto);
    boolean delete(String id);
    UnitDTO update(UnitDTO dto);

}
