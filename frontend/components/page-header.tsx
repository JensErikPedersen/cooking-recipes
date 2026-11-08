import type { ReactNode } from "react";

// A page's heading, with its actions - New, Edit, Delete - on the right. On a phone they stay on
// the heading's row as icons, and a long name wraps beside them; pt-1.5 sets its first line level
// with the 44 px buttons.
export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="flex items-start gap-2 md:items-center md:gap-3">
      <h1 className="min-w-0 flex-1 pt-1.5 text-2xl md:pt-0 md:text-4xl">{title}</h1>
      {children}
    </div>
  );
}
