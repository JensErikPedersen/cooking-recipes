"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ApiError, categories, type Category } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { ErrorMessage } from "@/components/error-message";
import { NotFound } from "@/components/not-found";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";

export default function CategoryPage({ params }: PageProps<"/categories/[id]">) {
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
      <PageHeader title={category.name}>
        <Link href={`/categories/${category.id}/edit`} className={secondaryButton}>
          Edit
        </Link>
        <DeleteButton
          what="category"
          name={category.name}
          remove={() => categories.remove(category.id)}
          listHref="/categories"
        />
      </PageHeader>
      <DetailList
        rows={[
          ["Name", category.name],
          ["Description", category.description],
        ]}
      />
    </>
  );
}
