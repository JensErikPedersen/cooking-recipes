"use client";

import { use } from "react";
import Link from "next/link";
import { recipes, type Recipe } from "@/lib/api";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { danish } from "@/lib/sort";

const link = "underline hover:no-underline";

export default function RecipePage({ params }: PageProps<"/recipes/[id]">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={recipes.get} notFoundTitle="Recipe not found" listHref="/recipes" listLabel="Back to recipes">
      {(recipe) => (
        <>
          <PageHeader title={recipe.name} />
          <DetailList
            rows={[
              [
                "Category",
                <Link key="category" href={`/categories/${recipe.category.id}`} className={link}>
                  {recipe.category.name}
                </Link>,
              ],
              ["Tags", <Tags key="tags" recipe={recipe} />],
              ["Description", recipe.description],
            ]}
          />
          <h2 className="mt-8 text-lg font-semibold">Ingredients</h2>
          <Ingredients recipe={recipe} />
          <h2 className="mt-8 text-lg font-semibold">Instructions</h2>
          {/* Instructions are free text with numbered lines; keep the line breaks. */}
          <p className="mt-2 whitespace-pre-line">{recipe.instructions || <None />}</p>
        </>
      )}
    </EntityLoader>
  );
}

function None() {
  return <span className="text-gray-500">None</span>;
}

function Tags({ recipe }: { recipe: Recipe }) {
  const tags = (recipe.tags ?? []).toSorted((a, b) => danish(a.name, b.name));
  if (tags.length === 0) {
    return <None />;
  }
  return (
    <ul className="flex flex-wrap gap-x-3">
      {tags.map((tag) => (
        <li key={tag.id}>
          <Link href={`/tags/${tag.id}`} className={link}>
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
      <p className="mt-2">
        <None />
      </p>
    );
  }
  return (
    <table className="mt-2">
      <tbody>
        {lines.map((line) => (
          <tr key={line.ingredientId}>
            <td className="py-1 pr-2 text-right">{line.amount}</td>
            <td className="py-1 pr-6">{line.unitLabel}</td>
            <td className="py-1">
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
