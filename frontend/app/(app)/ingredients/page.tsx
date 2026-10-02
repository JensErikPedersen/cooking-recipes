"use client";

import { ingredients } from "@/lib/api";
import { EntityList } from "@/components/entity-list";

export default function IngredientsPage() {
  return (
    <EntityList
      title="Ingredients"
      path="/ingredients"
      newLabel="New ingredient"
      load={ingredients.list}
      columns={[["Description", (ingredient) => ingredient.description]]}
    />
  );
}
