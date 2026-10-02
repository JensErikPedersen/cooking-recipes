"use client";

import { use } from "react";
import Link from "next/link";
import { ingredients } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";

export default function IngredientPage({ params }: PageProps<"/ingredients/[id]">) {
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
          <PageHeader title={ingredient.name}>
            <Link href={`/ingredients/${ingredient.id}/edit`} className={secondaryButton}>
              Edit
            </Link>
            <DeleteButton
              what="ingredient"
              name={ingredient.name}
              remove={() => ingredients.remove(ingredient.id)}
              listHref="/ingredients"
            />
          </PageHeader>
          <DetailList
            rows={[
              ["Name", ingredient.name],
              ["Description", ingredient.description],
            ]}
          />
        </>
      )}
    </EntityLoader>
  );
}
