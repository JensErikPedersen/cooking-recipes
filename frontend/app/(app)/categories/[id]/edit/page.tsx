"use client";

import { use } from "react";
import { categories } from "@/lib/api";
import { CategoryForm } from "@/components/category-form";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";

export default function EditCategoryPage({ params }: PageProps<"/categories/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader
      id={id}
      get={categories.get}
      notFoundTitle="Category not found"
      listHref="/categories"
      listLabel="Back to categories"
    >
      {(category) => (
        <>
          <PageHeader title="Edit category" />
          <CategoryForm category={category} />
        </>
      )}
    </EntityLoader>
  );
}
