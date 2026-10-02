"use client";

import { categories } from "@/lib/api";
import { EntityList } from "@/components/entity-list";

export default function CategoriesPage() {
  return (
    <EntityList
      title="Categories"
      path="/categories"
      newLabel="New category"
      load={categories.list}
      columns={[["Description", (category) => category.description]]}
    />
  );
}
