package dk.serik.recipes.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Objects;

@Data
public class BaseIdentityDTO extends BaseDTO {

    private String id;

    public BaseIdentityDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy) {
        super(created, createdBy, updated, updatedBy);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseIdentityDTO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
