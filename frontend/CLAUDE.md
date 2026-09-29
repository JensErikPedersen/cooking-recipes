@AGENTS.md

# Frontend

Next.js 16 App Router, TypeScript, Tailwind CSS 4, no component library. Project-wide rules live in
the root `CLAUDE.md`; this file holds only what is specific to `frontend/`.

## Conventions

- `app/` at the project root, no `src/`. Imports use the `@/*` alias.
- Styling is Tailwind utility classes; `app/globals.css` holds the theme variables.
- `/api` belongs to the backend: from Part 3 this server proxies it. Route handlers of this app
  live outside `/api`, like the placeholder `app/hello/route.ts`.
- The browser only ever talks to this origin, never to the backend directly. There is no CORS.
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
