"use client";

import { recipes } from "@/lib/api";
import { EntityList } from "@/components/entity-list";
import { Picture } from "@/components/picture";
import { categoryPill, tagPill } from "@/components/styles";
import { danish } from "@/lib/sort";

export default function RecipesPage() {
  return (
    <EntityList
      title="Recipes"
      path="/recipes"
      newLabel="New recipe"
      load={recipes.list}
      searchable
      // The full width of the label's field, 3:2, above the name; a card without one is unchanged.
      cardPicture={(recipe) =>
        recipe.picture && (
          <Picture picture={recipe.picture} place="card" lazy className="aspect-[3/2] w-full object-cover" />
        )
      }
      card={(recipe) => (
        <ul className="flex flex-wrap gap-2">
          <li className={categoryPill}>{recipe.category.name}</li>
          {(recipe.tags ?? []).toSorted((a, b) => danish(a.name, b.name)).map((tag) => (
            <li key={tag.id} className={tagPill}>
              {tag.name}
            </li>
          ))}
        </ul>
      )}
    />
  );
}
