package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;


@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeIngredientDTO extends BaseDTO{
	// recipeId comes from the path, not the body
	private String recipeId;

	@NotNull(message = "{dk.serik.models.recipeingredient.ingredientid.notnull.message}")
	private String ingredientId;

	private String ingredientName;

	@NotNull(message = "{dk.serik.models.recipeingredient.amount.notnull.message}")
	@DecimalMin(value = "0.0", inclusive = false, message = "{dk.serik.models.recipeingredient.amount.min.message}")
	private BigDecimal amount;

	@NotNull(message = "{dk.serik.models.recipeingredient.unitid.notnull.message}")
	private String unitId;
	private String unitLabel;

	@Builder
	@Jacksonized
	public RecipeIngredientDTO(String recipeId, String ingredientId, String ingredientName, BigDecimal amount, String unitId, String unitLabel, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy) {
		super(created, createdBy, updated, updatedBy);
		this.recipeId = recipeId;
		this.ingredientId = ingredientId;
		this.ingredientName = ingredientName;
		this.amount = amount;
		this.unitLabel = unitLabel;
		this.unitId = unitId;
	}

	@Override
	public String toString() {
		return "RecipeIngredientDTO{" +
				"recipeId='" + recipeId + '\'' +
				", ingredientId='" + ingredientId + '\'' +
				", ingredientName='" + ingredientName + '\'' +
				", amount=" + amount +
				", unitId='" + unitId + '\'' +
				", unitLabel='" + unitLabel + '\'' +
				", created=" + getCreated() +
				", createdBy='" + getCreatedBy() + '\'' +
				", updated=" + getUpdated() +
				", updatedBy='" + getUpdatedBy() + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RecipeIngredientDTO that)) return false;
		return Objects.equals(recipeId, that.recipeId) && Objects.equals(ingredientId, that.ingredientId) && Objects.equals(ingredientName, that.ingredientName) && Objects.equals(amount, that.amount) && Objects.equals(unitId, that.unitId) && Objects.equals(unitLabel, that.unitLabel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(recipeId, ingredientId, ingredientName, amount, unitId, unitLabel);
	}
}
