"use client";

import { use } from "react";
import Link from "next/link";
import { recipes, type Recipe } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList, None } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { ButtonLabel } from "@/components/icons";
import { Label } from "@/components/label";
import { categoryPill, iconButton, secondaryButton, tagPill, textLink as link } from "@/components/styles";
import { danish } from "@/lib/sort";

export default function RecipePage({ params }: PageProps<"/recipes/[id]">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={recipes.get} notFoundTitle="Recipe not found" listHref="/recipes" listLabel="Back to recipes">
      {(recipe) => (
        <>
          <PageHeader title={recipe.name}>
            <Link href={`/recipes/${recipe.id}/edit`} className={`${secondaryButton} ${iconButton}`}>
              <ButtonLabel icon="edit">Edit</ButtonLabel>
            </Link>
            <DeleteButton what="recipe" name={recipe.name} remove={() => recipes.remove(recipe.id)} listHref="/recipes" />
          </PageHeader>
          <DetailList
            rows={[
              [
                "Category",
                <Link key="category" href={`/categories/${recipe.category.id}`} className={`${categoryPill} hover:underline`}>
                  {recipe.category.name}
                </Link>,
              ],
              ["Tags", <Tags key="tags" recipe={recipe} />],
              ["Description", recipe.description],
            ]}
          />
          {/* Read at arm's length while cooking: 18 px. On a phone the ingredients come first; from md
              they stand beside the instructions and stay in view while the steps are read. */}
          <div className="mt-8 grid items-start gap-8 text-lg md:grid-cols-[minmax(15rem,19rem)_minmax(0,1fr)]">
            <Label className="md:sticky md:top-4">
              <h2 className="text-xl">Ingredients</h2>
              <Ingredients recipe={recipe} />
            </Label>
            <div>
              <h2 className="text-xl">Instructions</h2>
              {/* Instructions are free text with numbered lines; keep the line breaks. */}
              <p className="mt-3 max-w-[65ch] leading-[1.65] whitespace-pre-line">{recipe.instructions || <None />}</p>
            </div>
          </div>
        </>
      )}
    </EntityLoader>
  );
}

function Tags({ recipe }: { recipe: Recipe }) {
  const tags = (recipe.tags ?? []).toSorted((a, b) => danish(a.name, b.name));
  if (tags.length === 0) {
    return <None />;
  }
  return (
    <ul className="flex flex-wrap gap-2">
      {tags.map((tag) => (
        <li key={tag.id}>
          <Link href={`/tags/${tag.id}`} className={`${tagPill} hover:underline`}>
            {tag.name}
          </Link>
        </li>
      ))}
    </ul>
  );
}

// Amount and unit first, as a recipe reads: "500 gr Hvedemel". The API returns the lines unordered.
function Ingredients({ recipe }: { recipe: Recipe }) {
  const lines = (recipe.recipeIngredients ?? []).toSorted((a, b) => danish(a.ingredientName, b.ingredientName));
  if (lines.length === 0) {
    return (
      <p className="mt-3">
        <None />
      </p>
    );
  }
  return (
    <table className="mt-3">
      <tbody>
        {lines.map((line) => (
          <tr key={line.ingredientId}>
            <td className="py-1 pr-2 text-right align-baseline tabular-nums">{line.amount}</td>
            <td className="py-1 pr-4 align-baseline">{line.unitLabel}</td>
            <td className="py-1 align-baseline">
              <Link href={`/ingredients/${line.ingredientId}`} className={link}>
                {line.ingredientName}
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
