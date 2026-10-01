"use client";

import { useState } from "react";
import { categories, type Category } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { TextField } from "@/components/text-field";

// Create and edit alike: given a category it edits it, without one it creates one. No validation
// here - the server's rules and messages are the only ones, shown on the field they concern.
export function CategoryForm({ category }: { category?: Category }) {
  const [name, setName] = useState(category?.name ?? "");
  const [description, setDescription] = useState(category?.description ?? "");

  function save() {
    // A cleared description is sent as absent, so it is stored as none rather than as "".
    const input = { name, description: description || undefined };
    return category ? categories.update(category.id, input) : categories.create(input);
  }

  return (
    <EntityForm entity={category} path="/categories" save={save}>
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
        </>
      )}
    </EntityForm>
  );
}
