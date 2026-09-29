import type { NextConfig } from "next";

// Read during `next build`, not at runtime: the rewrite destination is baked into the build output.
// The Docker build passes BACKEND_URL as a build argument; `npm run dev` on the host falls back to
// the backend the stack publishes on localhost.
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Self-contained server.js plus only the files it needs, for the Docker image.
  output: "standalone",
  // The browser only ever talks to this origin. /api is forwarded server-side to the backend,
  // which is why there is no CORS configuration anywhere.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
