"use client";

import { use } from "react";
import { ingredients } from "@/lib/api";
import { EntityLoader } from "@/components/entity-loader";
import { IngredientForm } from "@/components/ingredient-form";
import { PageHeader } from "@/components/page-header";

export default function EditIngredientPage({ params }: PageProps<"/ingredients/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader
      id={id}
      get={ingredients.get}
      notFoundTitle="Ingredient not found"
      listHref="/ingredients"
      listLabel="Back to ingredients"
    >
      {(ingredient) => (
        <>
          <PageHeader title="Edit ingredient" />
          <IngredientForm ingredient={ingredient} />
        </>
      )}
    </EntityLoader>
  );
}
