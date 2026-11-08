"use client";

import { createContext, use, useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { MenuIcon } from "@/components/icons";
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
// page - and the logout button, and the page area below it; narrower than md, all three are in a
// panel behind the Menu button. It renders nothing until the backend confirms the session, so an
// expired session never shows a page.
export function AppShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  // The phone menu, a disclosure (docs/PLAN_V7.md, decision 5). From md the menu is always shown.
  const [open, setOpen] = useState(false);
  const menuButton = useRef<HTMLButtonElement>(null);

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

  // Escape closes the open menu and returns focus to its button.
  useEffect(() => {
    if (!open) {
      return;
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        menuButton.current?.focus();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open]);

  async function logout() {
    await auth.logout();
    router.push("/login");
  }

  if (!user) {
    return null;
  }

  return (
    <>
      {/* The olive band; its focus ring is cream, as olive would not show on it. Any link chosen in
          it - an entry, the name, the account - closes the phone menu. */}
      <header
        onClick={(event) => (event.target as Element).closest("a") && setOpen(false)}
        className={`bg-olive text-cream [&_:focus-visible]:outline-cream ${open ? "max-md:shadow-[0_10px_30px] max-md:shadow-olive/25" : ""}`}
      >
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-7 px-4 md:px-8">
          <Link href="/" className="py-3 font-display text-xl">
            Cooking Recipes
          </Link>
          {/* Narrower than md the menu is a panel, opened by this button and pushing the page down. */}
          <button
            ref={menuButton}
            type="button"
            aria-expanded={open}
            aria-controls="main-menu"
            onClick={() => setOpen(!open)}
            className="ml-auto inline-flex min-h-11 items-center gap-2 rounded-md border-[1.5px] border-cream px-3.5 font-semibold md:hidden"
          >
            <MenuIcon open={open} />
            Menu
          </button>
          {/* The panel: on a phone a column under the band; from md it is no box of its own
              (contents), and the menu and the account sit in the band's row. Between md and lg the
              menu takes a row of its own under the name and the account. */}
          <div
            id="main-menu"
            className={`max-md:basis-full max-md:pb-2 md:contents ${open ? "motion-safe:max-md:animate-open-down" : "max-md:hidden"}`}
          >
            <nav aria-label="Main" className="md:max-lg:order-last md:max-lg:basis-full">
              <ul className="flex flex-wrap gap-x-5.5 max-md:flex-col">
                {(isAdmin(user) ? [...NAVIGATION, ...ADMIN_NAVIGATION] : NAVIGATION).map(({ href, label }) => {
                  const current = pathname === href || pathname.startsWith(`${href}/`);
                  return (
                    <li key={href}>
                      {/* The current entry: an ochre bar under it, or beside it in the phone panel. */}
                      <Link
                        href={href}
                        aria-current={current ? "page" : undefined}
                        className={`font-semibold underline-offset-4 hover:underline max-md:flex max-md:min-h-12 max-md:items-center md:inline-block md:border-b-[3px] md:pt-1 md:pb-2 lg:pt-4.5 lg:pb-3.75 ${current ? "border-ochre max-md:border-l-4 max-md:pl-3" : "md:border-transparent"}`}
                      >
                        {label}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </nav>
            {/* The account's own page: its email address and password. */}
            <div className="flex items-center gap-3.5 max-md:mt-2 max-md:min-h-14 max-md:justify-between max-md:border-t-2 max-md:border-ochre max-md:pt-2 md:ml-auto">
              <Link href="/account" className="underline underline-offset-4 hover:no-underline">
                {user.username}
              </Link>
              <button
                type="button"
                onClick={logout}
                className="inline-flex min-h-11 items-center rounded-md border-[1.5px] border-cream px-3.5 font-semibold whitespace-nowrap hover:bg-cream/15 lg:min-h-10"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl px-4 pt-8 pb-16 md:px-8">
        <CurrentUser value={user}>{children}</CurrentUser>
      </main>
    </>
  );
}
