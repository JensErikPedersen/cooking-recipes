import { PageHeader } from "@/components/page-header";
import { RecipeForm } from "@/components/recipe-form";

export default function NewRecipePage() {
  return (
    <>
      <PageHeader title="New recipe" />
      <RecipeForm />
    </>
  );
}
