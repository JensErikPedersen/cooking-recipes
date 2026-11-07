"use client";

import { createContext, use, useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ApiError, auth, isAdmin, type AuthenticatedUser } from "@/lib/api";

// One entry per entity type; each slice adds its own when its pages exist.
const NAVIGATION = [
  { href: "/recipes", label: "Recipes" },
  { href: "/categories", label: "Categories" },
  { href: "/ingredients", label: "Ingredients" },
  { href: "/units", label: "Units" },
  { href: "/tags", label: "Tags" },
];

// For an admin only, after the entity types.
const ADMIN_NAVIGATION = [{ href: "/users", label: "Users" }];

const CurrentUser = createContext<AuthenticatedUser | null>(null);

/** The signed-in user, as AppShell confirmed it. Only inside AppShell, which renders nothing before. */
export function useCurrentUser(): AuthenticatedUser {
  return use(CurrentUser)!;
}

// The frame of every signed-in page: the header with the menu, the user - a link to their account
// page - and the logout button, and the page area below it. It renders nothing until the backend
// confirms the session, so an expired session never shows a page.
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
      {/* The olive band; its focus ring is cream, as olive would not show on it. */}
      <header className="bg-olive text-cream [&_:focus-visible]:outline-cream">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3 md:px-8">
          <Link href="/" className="font-display text-xl">
            Cooking Recipes
          </Link>
          {/* Narrower than md, the menu takes a row of its own below the title, the user and Log out. */}
          <nav aria-label="Main" className="order-last w-full md:order-none md:w-auto">
            <ul className="flex flex-wrap gap-x-4 gap-y-1">
              {(isAdmin(user) ? [...NAVIGATION, ...ADMIN_NAVIGATION] : NAVIGATION).map(({ href, label }) => {
                const current = pathname === href || pathname.startsWith(`${href}/`);
                return (
                  <li key={href}>
                    <Link
                      href={href}
                      aria-current={current ? "page" : undefined}
                      className={`font-semibold underline-offset-4 ${current ? "underline decoration-ochre decoration-[3px]" : "hover:underline"}`}
                    >
                      {label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
          {/* The account's own page: its email address and password. */}
          <Link href="/account" className="ml-auto underline underline-offset-4 hover:no-underline">
            {user.username}
          </Link>
          <button
            type="button"
            onClick={logout}
            className="inline-flex min-h-11 items-center rounded-md border-[1.5px] border-cream px-3.5 font-semibold hover:bg-cream/15 lg:min-h-10"
          >
            Log out
          </button>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl px-4 pt-8 pb-16 md:px-8">
        <CurrentUser value={user}>{children}</CurrentUser>
      </main>
    </>
  );
}
