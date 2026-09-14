package dk.serik.recipes.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("serial")
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecipeIngredientPK implements Serializable {
		
	private UUID recipe;
	
	private UUID ingredient;

	@Override
	public int hashCode() {
		return Objects.hash(ingredient, recipe);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecipeIngredientPK other = (RecipeIngredientPK) obj;
		return Objects.equals(ingredient, other.ingredient) && Objects.equals(recipe, other.recipe);
	}
	
	

//	@Override
//	public int hashCode() {
//		return Objects.hash(ingredientEntity, recipeEntity);
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		RecipeIngredientKey other = (RecipeIngredientKey) obj;
//		return Objects.equals(ingredientEntity, other.ingredientEntity)
//				&& Objects.equals(recipeEntity, other.recipeEntity);
//	}
	
	
	

//	@Transient
//	private static final long serialVersionUID = -1437096901351465476L;
//	
//	@Column( updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
//	@Type(type = "uuid-char")
//	private UUID recipeId;
//	private RecipeEntity recipeEntity;
	    
//    @Column( updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
//    @Type(type = "uuid-char")
//    private UUID ingredientId;
//    
//	private IngredientEntity ingredientEntity;
    
//    public RecipeIngredientKey(UUID recipeId, UUID ingredientId) {
//    	log.info("Setting recipeId: {} and ingredientId: {}", recipeId, ingredientId);
//    	this.recipeId = recipeId;
//    	this.ingredientId = ingredientId;
//    }
//
//    public RecipeIngredientKey() {}
//    
//	public UUID getRecipeId() {
//		return recipeId;
//	}
//
//	public void setRecipeId(UUID recipeId) {
//		log.info("Set recipeId: {}", recipeId);
//		this.recipeId = recipeId;
//	}
//
//	public UUID getIngredientId() {
//		return ingredientId;
//	}
//
//	public void setIngredientId(UUID ingredientId) {
//		log.info("Set ingredientId: {}", ingredientId);
//		this.ingredientId = ingredientId;
//	}
//
//	@Override
//	public String toString() {
//		return "RecipeIngredientKey [recipeId=" + recipeId + ", ingredientId=" + ingredientId + "]";
//	}
//
//	@Override
//	public int hashCode() {
//		return Objects.hash(ingredientId, recipeId);
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		RecipeIngredientKey other = (RecipeIngredientKey) obj;
//		return Objects.equals(ingredientId, other.ingredientId) && Objects.equals(recipeId, other.recipeId);
//	}
//
//    
	
	
    
}
