"use client";

import { useState } from "react";
import { ingredients, type Ingredient } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { PictureField } from "@/components/picture-field";
import { TextField } from "@/components/text-field";

// Create and edit alike; see CategoryForm, which has the same two fields - and a picture here.
export function IngredientForm({ ingredient }: { ingredient?: Ingredient }) {
  const [name, setName] = useState(ingredient?.name ?? "");
  const [description, setDescription] = useState(ingredient?.description ?? "");
  const [picture, setPicture] = useState(ingredient?.picture);

  function save() {
    // A cleared description is sent as absent, so it is stored as none rather than as "".
    const input = { name, description: description || undefined, picture: picture && { id: picture.id } };
    return ingredient ? ingredients.update(ingredient.id, input) : ingredients.create(input);
  }

  return (
    <EntityForm entity={ingredient} path="/ingredients" save={save}>
      {(errors) => (
        <>
          <TextField label="Name" name="name" value={name} onChange={setName} error={errors.name} />
          <TextField
            label="Description"
            name="description"
            value={description}
            onChange={setDescription}
            error={errors.description}
            multiline
          />
          <PictureField picture={picture} onChange={setPicture} error={errors.picture} />
        </>
      )}
    </EntityForm>
  );
}
