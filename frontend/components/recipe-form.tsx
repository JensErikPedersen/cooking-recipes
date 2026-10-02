"use client";

import { useEffect, useState } from "react";
import { categories, recipes, tags, type Category, type Recipe, type RecipeInput, type Tag } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { ErrorMessage } from "@/components/error-message";
import { SelectField } from "@/components/select-field";
import { TextField } from "@/components/text-field";
import { danish } from "@/lib/sort";

// Create and edit alike. The category and the tags are chosen from the existing ones, which load
// first; the form renders once they have.
export function RecipeForm({ recipe }: { recipe?: Recipe }) {
  const [choices, setChoices] = useState<{ categories: Category[]; tags: Tag[] }>();
  const [loadError, setLoadError] = useState<unknown>();
  const [name, setName] = useState(recipe?.name ?? "");
  const [description, setDescription] = useState(recipe?.description ?? "");
  const [instructions, setInstructions] = useState(recipe?.instructions ?? "");
  const [categoryId, setCategoryId] = useState(recipe?.category.id ?? "");
  const [tagIds, setTagIds] = useState<string[]>(recipe?.tags?.map((tag) => tag.id) ?? []);

  useEffect(() => {
    Promise.all([categories.list(), tags.list()]).then(
      ([categoryList, tagList]) => setChoices({ categories: categoryList, tags: tagList }),
      setLoadError,
    );
  }, []);

  if (loadError) {
    return <ErrorMessage error={loadError} />;
  }
  if (!choices) {
    return null;
  }

  function save() {
    // Built field by field, never from the loaded recipe: that one carries recipeRatings, which the
    // API rejects on write. Cleared text is sent as absent, so it is stored as none.
    const input: RecipeInput = {
      name,
      description: description || undefined,
      instructions: instructions || undefined,
      category: categoryId ? { id: categoryId } : undefined,
      tags: tagIds.map((id) => ({ id })),
    };
    return recipe ? recipes.update(recipe.id, input) : recipes.create(input);
  }

  function toggleTag(id: string, checked: boolean) {
    setTagIds((current) => (checked ? [...current, id] : current.filter((tagId) => tagId !== id)));
  }

  return (
    <EntityForm entity={recipe} path="/recipes" save={save}>
      {(errors) => (
        <>
          <TextField label="Name" name="name" value={name} onChange={setName} error={errors.name} />
          <SelectField
            label="Category"
            name="category"
            value={categoryId}
            onChange={setCategoryId}
            options={choices.categories
              .toSorted((a, b) => danish(a.name, b.name))
              .map((category) => ({ value: category.id, label: category.name }))}
            placeholder="Choose a category"
            error={errors.category}
          />
          <fieldset className="flex flex-col gap-1">
            <legend className="font-semibold">Tags</legend>
            <div className="flex flex-wrap gap-x-5 gap-y-1">
              {choices.tags
                .toSorted((a, b) => danish(a.name, b.name))
                .map((tag) => (
                  <label key={tag.id} className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={tagIds.includes(tag.id)}
                      onChange={(event) => toggleTag(tag.id, event.target.checked)}
                    />
                    {tag.name}
                  </label>
                ))}
            </div>
          </fieldset>
          <TextField
            label="Description"
            name="description"
            value={description}
            onChange={setDescription}
            error={errors.description}
            multiline
          />
          <TextField
            label="Instructions"
            name="instructions"
            value={instructions}
            onChange={setInstructions}
            error={errors.instructions}
            multiline
          />
        </>
      )}
    </EntityForm>
  );
}
