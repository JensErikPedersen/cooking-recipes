"use client";

import { recipes } from "@/lib/api";
import { EntityList } from "@/components/entity-list";
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
