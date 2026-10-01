import { APIRequestContext, expect } from "@playwright/test";

/**
 * Signs the given request context in, the way the frontend will: fetch the XSRF-TOKEN cookie, then
 * POST the credentials with the token in the X-XSRF-TOKEN header. The context keeps the session
 * cookie for every later call. Credentials are the stack's own, from the repository's .env.
 */
export async function signInThroughApi(request: APIRequestContext) {
  await request.get("/api/v1/auth/me"); // a 401, but it issues the XSRF-TOKEN cookie
  const { cookies } = await request.storageState();
  const xsrf = cookies.find((cookie) => cookie.name === "XSRF-TOKEN");
  expect(xsrf, "XSRF-TOKEN cookie").toBeDefined();

  const response = await request.post("/api/v1/auth/login", {
    headers: { "X-XSRF-TOKEN": xsrf!.value },
    data: { username: process.env.APP_ADMIN_USERNAME, password: process.env.APP_ADMIN_PASSWORD },
  });
  expect(response.status(), "login").toBe(200);
}
