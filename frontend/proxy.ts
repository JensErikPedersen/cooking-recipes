import { NextResponse, type NextRequest } from "next/server";

// Sends a visitor without a session cookie to /login before any page renders. A convenience, not
// the security: it cannot tell a valid session from an expired one - AppShell's check catches
// that - and the backend answers 401 to every /api call without a session regardless.
export function proxy(request: NextRequest) {
  return NextResponse.redirect(new URL("/login", request.url));
}

export const config = {
  matcher: [
    {
      // Everything except /api (the backend's to answer), /login itself and Next's own assets.
      source: "/((?!api|login|_next/static|_next/image|favicon.ico).*)",
      missing: [{ type: "cookie", key: "JSESSIONID" }],
    },
  ],
};
