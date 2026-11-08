import type { ReactNode } from "react";

// The jar's label (docs/PLAN_V7.md, "Shapes"), drawn by .label and .label-field in globals.css:
// for what is opened or read as one thing - a recipe card, a recipe's ingredients.
export function Label({ as: Tag = "div", className = "", fieldClassName = "", children }: {
  as?: "div" | "li";
  className?: string;
  fieldClassName?: string;
  children: ReactNode;
}) {
  return (
    <Tag className={`label ${className}`}>
      <div className={`label-field ${fieldClassName}`}>{children}</div>
    </Tag>
  );
}
