import { IngredientForm } from "@/components/ingredient-form";
import { PageHeader } from "@/components/page-header";

export default function NewIngredientPage() {
  return (
    <>
      <PageHeader title="New ingredient" />
      <IngredientForm />
    </>
  );
}
