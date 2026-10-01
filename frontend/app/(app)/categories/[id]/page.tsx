"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ApiError, categories, type Category } from "@/lib/api";
import { DetailList } from "@/components/detail-list";
import { ErrorMessage } from "@/components/error-message";

export default function CategoryPage({ params }: PageProps<"/categories/[id]">) {
  const { id } = use(params);
  const [category, setCategory] = useState<Category>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    categories.get(id).then(setCategory, setError);
  }, [id]);

  if (error instanceof ApiError && error.status === 404) {
    return (
      <>
        <h1 className="text-2xl font-bold">Category not found</h1>
        <p className="mt-4">
          It may have been deleted.{" "}
          <Link href="/categories" className="underline hover:no-underline">
            Back to categories
          </Link>
        </p>
      </>
    );
  }
  if (error) {
    return <ErrorMessage error={error} />;
  }
  if (!category) {
    return null;
  }

  return (
    <>
      <h1 className="text-2xl font-bold">{category.name}</h1>
      <DetailList
        rows={[
          ["Name", category.name],
          ["Description", category.description],
        ]}
      />
    </>
  );
}
