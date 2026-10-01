import type { ReactNode } from "react";

// The read-only view of an entity: one labelled row per field. An empty value reads "None" rather
// than leaving a gap that looks like a rendering fault.
export function DetailList({ rows }: { rows: [label: string, value: ReactNode][] }) {
  return (
    <dl className="mt-6 grid grid-cols-[max-content_1fr] gap-x-8 gap-y-3">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="font-semibold">{label}</dt>
          <dd>{value || <span className="text-gray-500">None</span>}</dd>
        </div>
      ))}
    </dl>
  );
}
