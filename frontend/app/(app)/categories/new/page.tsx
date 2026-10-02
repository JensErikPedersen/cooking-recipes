import { CategoryForm } from "@/components/category-form";
import { PageHeader } from "@/components/page-header";

export default function NewCategoryPage() {
  return (
    <>
      <PageHeader title="New category" />
      <CategoryForm />
    </>
  );
}
