import type { Ingredient, Unit } from "@/lib/api";
import { secondaryButton } from "@/components/styles";
import { danish } from "@/lib/sort";

/** A line as the form edits it: what the inputs hold, the amount still as typed. */
export interface Line {
  key: string;
  ingredientId: string;
  amount: string;
  unitId: string;
}

let nextKey = 0;
export const newLine = (ingredientId = "", amount = "", unitId = ""): Line => ({
  key: String(nextKey++),
  ingredientId,
  amount,
  unitId,
});

/**
 * The amount as the API takes it. A comma is read as the decimal mark, as a Danish reader types
 * it; anything that is not a number is sent as no amount, and the server says so.
 */
export function parseAmount(amount: string): number | undefined {
  const value = Number(amount.trim().replace(",", "."));
  return amount.trim() === "" || Number.isNaN(value) ? undefined : value;
}

// The ingredient lines of the recipe form: one row per line - ingredient, amount, unit - with Add
// and Remove. Each row is a group named "Ingredient line N", its inputs labelled within it. The
// server reports every line problem in one message, shown under the section.
export function IngredientLines({
  lines,
  onChange,
  ingredients,
  units,
  error,
}: {
  lines: Line[];
  onChange: (lines: Line[]) => void;
  ingredients: Ingredient[];
  units: Unit[];
  error?: string;
}) {
  const set = (key: string, change: Partial<Line>) =>
    onChange(lines.map((line) => (line.key === key ? { ...line, ...change } : line)));
  const control = "rounded border px-3 py-2";

  return (
    <fieldset
      // min-w-0: a fieldset is at least as wide as its content by default, which pushed the lines
      // past the page margin on a phone.
      className="flex min-w-0 flex-col gap-2"
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? "recipeIngredients-error" : undefined}
    >
      <legend className="font-semibold">Ingredients</legend>
      {lines.map((line, index) => (
        <div
          key={line.key}
          role="group"
          aria-label={`Ingredient line ${index + 1}`}
          // Narrower than sm a line takes two rows: the ingredient, then amount, unit and Remove.
          className="grid grid-cols-[5rem_1fr_auto] gap-2 sm:grid-cols-[1fr_6rem_10rem_auto]"
        >
          <label className="sr-only" htmlFor={`ingredient-${line.key}`}>
            Ingredient
          </label>
          <select
            id={`ingredient-${line.key}`}
            value={line.ingredientId}
            onChange={(event) => set(line.key, { ingredientId: event.target.value })}
            className={`${control} col-span-3 sm:col-span-1`}
          >
            <option value="">Choose an ingredient</option>
            {ingredients
              .toSorted((a, b) => danish(a.name, b.name))
              .map((ingredient) => (
                <option key={ingredient.id} value={ingredient.id}>
                  {ingredient.name}
                </option>
              ))}
          </select>
          <label className="sr-only" htmlFor={`amount-${line.key}`}>
            Amount
          </label>
          <input
            id={`amount-${line.key}`}
            inputMode="decimal"
            value={line.amount}
            onChange={(event) => set(line.key, { amount: event.target.value })}
            className={control}
          />
          <label className="sr-only" htmlFor={`unit-${line.key}`}>
            Unit
          </label>
          <select
            id={`unit-${line.key}`}
            value={line.unitId}
            onChange={(event) => set(line.key, { unitId: event.target.value })}
            className={control}
          >
            <option value="">Choose a unit</option>
            {units
              .toSorted((a, b) => danish(a.name, b.name))
              .map((unit) => (
                <option key={unit.id} value={unit.id}>
                  {unit.name} ({unit.label})
                </option>
              ))}
          </select>
          <button
            type="button"
            onClick={() => onChange(lines.filter((other) => other.key !== line.key))}
            className={secondaryButton}
          >
            Remove
          </button>
        </div>
      ))}
      <div>
        <button type="button" onClick={() => onChange([...lines, newLine()])} className={secondaryButton}>
          Add ingredient
        </button>
      </div>
      {error && (
        <p id="recipeIngredients-error" className="text-sm text-red-700">
          {error}
        </p>
      )}
    </fieldset>
  );
}
