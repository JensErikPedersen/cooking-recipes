// Placeholder for Part 2: proves the browser can fetch from the Next.js server.
// Deliberately outside /api, which Part 3 proxies to the backend.
export function GET() {
  return Response.json({ message: "Hello from the Next.js server" });
}
