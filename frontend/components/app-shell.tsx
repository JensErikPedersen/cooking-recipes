"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ApiError, auth, type AuthenticatedUser } from "@/lib/api";

// One entry per entity type; each slice adds its own when its pages exist.
const NAVIGATION = [
  { href: "/recipes", label: "Recipes" },
  { href: "/categories", label: "Categories" },
  { href: "/ingredients", label: "Ingredients" },
  { href: "/units", label: "Units" },
  { href: "/tags", label: "Tags" },
];

// The frame of every signed-in page: the header with the menu, the user and the logout button, and
// the page area below it. It renders nothing until the backend confirms the session, so an expired
// session never shows a page.
export function AppShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<AuthenticatedUser | null>(null);

  useEffect(() => {
    auth
      .me()
      .then(setUser)
      .catch((error: unknown) => {
        // A 401 has already sent the browser to /login, from lib/api.ts.
        if (!(error instanceof ApiError && error.status === 401)) {
          throw error;
        }
      });
  }, []);

  async function logout() {
    await auth.logout();
    router.push("/login");
  }

  if (!user) {
    return null;
  }

  return (
    <>
      <header className="border-b">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center gap-x-6 gap-y-2 px-8 py-3">
          <Link href="/" className="text-lg font-semibold">
            Cooking Recipes
          </Link>
          {/* Narrower than md, the menu takes a row of its own below the title, the user and Log out. */}
          <nav aria-label="Main" className="order-last w-full md:order-none md:w-auto">
            <ul className="flex flex-wrap gap-x-4 gap-y-1">
              {NAVIGATION.map(({ href, label }) => {
                const current = pathname === href || pathname.startsWith(`${href}/`);
                return (
                  <li key={href}>
                    <Link
                      href={href}
                      aria-current={current ? "page" : undefined}
                      className={current ? "font-semibold underline" : "hover:underline"}
                    >
                      {label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
          <span className="ml-auto text-sm">{user.username}</span>
          <button type="button" onClick={logout} className="rounded border px-3 py-1 text-sm hover:bg-gray-100">
            Log out
          </button>
        </div>
      </header>
      <main className="mx-auto w-full max-w-4xl px-8 py-6">{children}</main>
    </>
  );
}
