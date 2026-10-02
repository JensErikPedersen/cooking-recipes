"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { units, type Unit } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { PageHeader } from "@/components/page-header";
import { primaryButton } from "@/components/styles";

export default function UnitsPage() {
  const [list, setList] = useState<Unit[]>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    units.list().then(setList, setError);
  }, []);

  return (
    <>
      <PageHeader title="Units">
        <Link href="/units/new" className={primaryButton}>
          New unit
        </Link>
      </PageHeader>
      {error ? (
        <ErrorMessage error={error} />
      ) : (
        list && (
          <table className="mt-6 w-full text-left">
            <thead className="border-b">
              <tr>
                <th className="py-2 pr-8">Name</th>
                <th className="py-2">Label</th>
              </tr>
            </thead>
            <tbody>
              {/* The API returns them in storage order. */}
              {list
                .toSorted((a, b) => a.name.localeCompare(b.name))
                .map((unit) => (
                  <tr key={unit.id} className="border-b">
                    <td className="py-2 pr-8">
                      <Link href={`/units/${unit.id}`} className="underline hover:no-underline">
                        {unit.name}
                      </Link>
                    </td>
                    <td className="py-2">{unit.label}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        )
      )}
    </>
  );
}
