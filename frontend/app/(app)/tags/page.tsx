"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { tags, type Tag } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { PageHeader } from "@/components/page-header";
import { primaryButton } from "@/components/styles";

export default function TagsPage() {
  const [list, setList] = useState<Tag[]>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    tags.list().then(setList, setError);
  }, []);

  return (
    <>
      <PageHeader title="Tags">
        <Link href="/tags/new" className={primaryButton}>
          New tag
        </Link>
      </PageHeader>
      {error ? (
        <ErrorMessage error={error} />
      ) : (
        list && (
          <table className="mt-6 w-full text-left">
            <thead className="border-b">
              <tr>
                <th className="py-2">Name</th>
              </tr>
            </thead>
            <tbody>
              {/* The API returns them in storage order. */}
              {list
                .toSorted((a, b) => a.name.localeCompare(b.name))
                .map((tag) => (
                  <tr key={tag.id} className="border-b">
                    <td className="py-2">
                      <Link href={`/tags/${tag.id}`} className="underline hover:no-underline">
                        {tag.name}
                      </Link>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        )
      )}
    </>
  );
}
