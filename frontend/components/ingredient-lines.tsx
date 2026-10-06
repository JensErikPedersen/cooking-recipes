import type { ReactNode } from "react";
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
// server reports a problem on its line and input, "recipeIngredients[2].amount", and it is shown
// under that input - tied to it like a TextField's error.
export function IngredientLines({
  lines,
  onChange,
  ingredients,
  units,
  errors,
}: {
  lines: Line[];
  onChange: (lines: Line[]) => void;
  ingredients: Ingredient[];
  units: Unit[];
  /** The server's errors by field name; this reads the recipeIngredients[n] ones. */
  errors: Record<string, string>;
}) {
  const set = (key: string, change: Partial<Line>) =>
    onChange(lines.map((line) => (line.key === key ? { ...line, ...change } : line)));

  return (
    <fieldset
      // min-w-0: a fieldset is at least as wide as its content by default, which pushed the lines
      // past the page margin on a phone.
      className="flex min-w-0 flex-col gap-2"
    >
      <legend className="font-semibold">Ingredients</legend>
      {lines.map((line, index) => {
        const error = (input: string) => errors[`recipeIngredients[${index}].${input}`];
        return (
          <div
            key={line.key}
            role="group"
            aria-label={`Ingredient line ${index + 1}`}
            // Narrower than sm a line takes two rows: the ingredient, then amount, unit and Remove.
            className="grid grid-cols-[5rem_1fr_auto] items-start gap-2 sm:grid-cols-[1fr_6rem_10rem_auto]"
          >
            <LineInput label="Ingredient" id={`ingredient-${line.key}`} error={error("ingredientId")} wide>
              {(props) => (
                <select
                  {...props}
                  value={line.ingredientId}
                  onChange={(event) => set(line.key, { ingredientId: event.target.value })}
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
              )}
            </LineInput>
            <LineInput label="Amount" id={`amount-${line.key}`} error={error("amount")}>
              {(props) => (
                <input
                  {...props}
                  inputMode="decimal"
                  value={line.amount}
                  onChange={(event) => set(line.key, { amount: event.target.value })}
                />
              )}
            </LineInput>
            <LineInput label="Unit" id={`unit-${line.key}`} error={error("unitId")}>
              {(props) => (
                <select {...props} value={line.unitId} onChange={(event) => set(line.key, { unitId: event.target.value })}>
                  <option value="">Choose a unit</option>
                  {units
                    .toSorted((a, b) => danish(a.name, b.name))
                    .map((unit) => (
                      <option key={unit.id} value={unit.id}>
                        {unit.name} ({unit.label})
                      </option>
                    ))}
                </select>
              )}
            </LineInput>
            <button
              type="button"
              onClick={() => onChange(lines.filter((other) => other.key !== line.key))}
              className={secondaryButton}
            >
              Remove
            </button>
          </div>
        );
      })}
      <div>
        <button type="button" onClick={() => onChange([...lines, newLine()])} className={secondaryButton}>
          Add ingredient
        </button>
      </div>
    </fieldset>
  );
}

// One input of a line: a label for screen readers, the input, and the server's error under it.
function LineInput({
  label,
  id,
  error,
  wide = false,
  children,
}: {
  label: string;
  id: string;
  error?: string;
  /** Takes the whole first row on a phone, as the ingredient does. */
  wide?: boolean;
  children: (props: {
    id: string;
    className: string;
    "aria-invalid"?: boolean;
    "aria-describedby"?: string;
  }) => ReactNode;
}) {
  const errorId = `${id}-error`;
  return (
    <div className={`flex min-w-0 flex-col gap-1 ${wide ? "col-span-3 sm:col-span-1" : ""}`}>
      <label className="sr-only" htmlFor={id}>
        {label}
      </label>
      {children({
        id,
        className: "rounded border px-3 py-2",
        "aria-invalid": error ? true : undefined,
        "aria-describedby": error ? errorId : undefined,
      })}
      {error && (
        <p id={errorId} className="text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
