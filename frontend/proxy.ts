import { NextResponse, type NextRequest } from "next/server";
import { loginPath } from "@/lib/return-to";

// Sends a visitor without a session cookie to /login before any page renders, remembering the page
// asked for so signing in can return to it. A convenience, not
// the security: it cannot tell a valid session from an expired one - AppShell's check catches
// that - and the backend answers 401 to every /api call without a session regardless.
export function proxy(request: NextRequest) {
  return NextResponse.redirect(new URL(loginPath(request.nextUrl.pathname + request.nextUrl.search), request.url));
}

export const config = {
  matcher: [
    {
      // Everything except /api (the backend's to answer), the two pages for a visitor without an
      // account - /login and /register - and Next's own assets.
      source: "/((?!api|login|register|_next/static|_next/image|favicon.ico).*)",
      missing: [{ type: "cookie", key: "JSESSIONID" }],
    },
  ],
};
