"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { ApiError, auth, type AuthenticatedUser } from "@/lib/api";

// The frame of every signed-in page: the header with the user and the logout button. It renders
// nothing until the backend confirms the session, so an expired session never shows a page.
// Part 5 adds the navigation menu here.
export function AppShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthenticatedUser | null>(null);

  useEffect(() => {
    auth
      .me()
      .then(setUser)
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
        } else {
          throw error;
        }
      });
  }, [router]);

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
        <div className="mx-auto flex max-w-4xl items-center gap-4 px-8 py-3">
          <span className="text-lg font-semibold">Cooking Recipes</span>
          <span className="ml-auto text-sm">{user.username}</span>
          <button type="button" onClick={logout} className="rounded border px-3 py-1 text-sm hover:bg-gray-100">
            Log out
          </button>
        </div>
      </header>
      {children}
    </>
  );
}
