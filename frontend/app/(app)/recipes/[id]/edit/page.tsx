"use client";

import { use } from "react";
import { recipes } from "@/lib/api";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { RecipeForm } from "@/components/recipe-form";

export default function EditRecipePage({ params }: PageProps<"/recipes/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={recipes.get} notFoundTitle="Recipe not found" listHref="/recipes" listLabel="Back to recipes">
      {(recipe) => (
        <>
          <PageHeader title="Edit recipe" />
          <RecipeForm recipe={recipe} />
        </>
      )}
    </EntityLoader>
  );
}
