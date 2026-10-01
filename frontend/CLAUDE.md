@AGENTS.md

# Frontend

Next.js 16 App Router, TypeScript, Tailwind CSS 4, no component library. Project-wide rules live in
the root `CLAUDE.md`; this file holds only what is specific to `frontend/`.

## Conventions

- `app/` at the project root, no `src/`. Imports use the `@/*` alias.
- Styling is Tailwind utility classes; `app/globals.css` holds the theme variables.
- `/api` belongs to the backend: `rewrites()` in `next.config.ts` forwards `/api/:path*` to it.
  Route handlers of this app live outside `/api`, like the placeholder `app/hello/route.ts`.
- The rewrite destination is fixed at **build** time: `next build` evaluates `next.config.ts` and
  writes the URL into `.next/routes-manifest.json`. So `BACKEND_URL` is a Docker build argument,
  derived by Compose from `APPLICATION_PORT`; setting it on the running container does nothing.
  `npm run dev` falls back to `http://localhost:8080`, the backend the stack publishes.
- The browser only ever talks to this origin, never to the backend directly. There is no CORS.
- All backend calls go through `lib/api.ts`. Writes send the CSRF token from the `XSRF-TOKEN`
  cookie as `X-XSRF-TOKEN`; logout clears that cookie, so a write that finds none fetches a fresh
  one first. Non-2xx responses throw `ApiError`, carrying the backend's error envelope.
- Signed-in pages live in the `app/(app)/` route group, whose layout wraps them in `AppShell`: it
  asks `/api/v1/auth/me` and renders nothing until the session is confirmed, sending a 401 to
  `/login`. `proxy.ts` (Next 16's name for middleware) redirects requests without a `JSESSIONID`
  cookie before any page renders. Neither is the security - the backend answers 401 regardless.
- Data fetching: the placeholder uses a plain `fetch` in a client component. The pattern for real
  pages is decided in Part 5 and recorded here.
- No frontend unit tests. Behaviour is covered by Playwright in `/e2e`, which asserts what a user
  sees - roles and visible text - rather than markup.

## Versions

- React follows the latest 19.x. ESLint stays on 9 until the plugins inside `eslint-config-next`
  accept 10.
- The host runs Node 22 for `npm run dev`; the image runs Node 24.

## Docker

- `output: "standalone"` in `next.config.ts`; the image runs `node server.js` as user `node`.
- There is no `public/` directory. Adding one needs a matching `COPY` in the `Dockerfile`: the
  standalone output leaves it out, and its files would 404 in the container while working under
  `npm run dev`.
- `.dockerignore` keeps the host `node_modules` out of the build; it holds Windows binaries.
- `npm run dev` and the stack both use port 3000. Stop one before starting the other.
