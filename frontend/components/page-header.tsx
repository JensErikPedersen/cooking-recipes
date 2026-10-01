import type { ReactNode } from "react";

// A page's heading, with its actions - New, Edit, Delete - on the right.
export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <h1 className="mr-auto text-2xl font-bold">{title}</h1>
      {children}
    </div>
  );
}
