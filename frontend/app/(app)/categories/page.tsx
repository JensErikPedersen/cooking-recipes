"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { categories, type Category } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { PageHeader } from "@/components/page-header";
import { primaryButton } from "@/components/styles";

export default function CategoriesPage() {
  const [list, setList] = useState<Category[]>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    categories.list().then(setList, setError);
  }, []);

  return (
    <>
      <PageHeader title="Categories">
        <Link href="/categories/new" className={primaryButton}>
          New category
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
                <th className="py-2">Description</th>
              </tr>
            </thead>
            <tbody>
              {/* The API returns them in storage order. */}
              {list
                .toSorted((a, b) => a.name.localeCompare(b.name))
                .map((category) => (
                  <tr key={category.id} className="border-b">
                    <td className="py-2 pr-8">
                      <Link href={`/categories/${category.id}`} className="underline hover:no-underline">
                        {category.name}
                      </Link>
                    </td>
                    <td className="py-2">{category.description}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        )
      )}
    </>
  );
}
