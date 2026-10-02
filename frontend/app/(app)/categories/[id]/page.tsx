"use client";

import { use } from "react";
import Link from "next/link";
import { categories } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";

export default function CategoryPage({ params }: PageProps<"/categories/[id]">) {
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
      )}
    </EntityLoader>
  );
}
