"use client";

import { recipes } from "@/lib/api";
import { EntityList } from "@/components/entity-list";
import { danish } from "@/lib/sort";

export default function RecipesPage() {
  return (
    <EntityList
      title="Recipes"
      path="/recipes"
      load={recipes.list}
      columns={[
        ["Category", (recipe) => recipe.category.name],
        ["Tags", (recipe) => (recipe.tags ?? []).map((tag) => tag.name).toSorted(danish).join(", ")],
      ]}
    />
  );
}
