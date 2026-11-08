import type { ReactNode } from "react";

// The read-only view of an entity: one labelled row per field. An empty value reads "None" rather
// than leaving a gap that looks like a rendering fault.
export function DetailList({ rows }: { rows: [label: string, value: ReactNode][] }) {
  return (
    <dl className="mt-6 grid grid-cols-[max-content_minmax(0,1fr)] items-baseline gap-x-7 gap-y-3">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="font-semibold">{label}</dt>
          <dd className="break-words">{value || <None />}</dd>
        </div>
      ))}
    </dl>
  );
}

// An empty value, in italics: text is never lightened (docs/PLAN_V7.md, decision 1).
export function None() {
  return <span className="italic">None</span>;
}
