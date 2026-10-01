"use client";

import { use, useEffect, useState } from "react";
import { ApiError, categories, type Category } from "@/lib/api";
import { CategoryForm } from "@/components/category-form";
import { ErrorMessage } from "@/components/error-message";
import { NotFound } from "@/components/not-found";
import { PageHeader } from "@/components/page-header";

export default function EditCategoryPage({ params }: PageProps<"/categories/[id]/edit">) {
  const { id } = use(params);
  const [category, setCategory] = useState<Category>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    categories.get(id).then(setCategory, setError);
  }, [id]);

  if (error instanceof ApiError && error.status === 404) {
    return <NotFound title="Category not found" listHref="/categories" listLabel="Back to categories" />;
  }
  if (error) {
    return <ErrorMessage error={error} />;
  }
  if (!category) {
    return null;
  }

  return (
    <>
      <PageHeader title="Edit category" />
      <CategoryForm category={category} />
    </>
  );
}
