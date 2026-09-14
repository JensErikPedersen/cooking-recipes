package dk.serik.recipes.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@MappedSuperclass
@EntityListeners(BaseEntityListener.class)
@Slf4j
public class BaseEntity implements Serializable {

	    @Setter(AccessLevel.PRIVATE)
	    @Column(nullable = false)
	    protected OffsetDateTime created;

	    @Column(nullable = false)
	    protected String createdBy;

		@Setter(AccessLevel.PRIVATE)
		@Column(nullable = false)
	    protected OffsetDateTime updated;

		@Column(nullable = false)
	    protected String updatedBy;

	    @PrePersist
	    public void prePersist() {
	        setCreated(OffsetDateTime.now());
	    }
	    @PostPersist
	    public void postPersist() {
	    }
	    @PreRemove
	    public void preRemove() {
	    }
	    @PostRemove
	    public void postRemove() {
	    }
	    @PreUpdate
	    public void preUpdate() {
	    	setUpdated(OffsetDateTime.now());	        
	    }
	    @PostUpdate
	    public void postUpdate() {
	    }
	    @PostLoad
	    public void postLoad() {
	    }

	    // Deliberately not Comparable. The previous raw implementation returned -1 for null, for a
	    // wrong type, and for two entities with null timestamps - breaking both the
	    // NullPointerException requirement and antisymmetry, which makes any sort or TreeSet
	    // containing these undefined. Nothing sorts entities; if something needs to, pass an
	    // explicit Comparator rather than reinstating a natural ordering here.
}
