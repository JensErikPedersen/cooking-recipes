import { expect } from "@playwright/test";

// The stack's Mailpit, which catches every mail the backend sends. Its API is published on this
// machine only, as the web page is.
const MAILPIT = process.env.MAILPIT_URL ?? "http://localhost:8025";

interface Summary {
  ID: string;
}

/** The mails to this address, newest first. */
export async function mailsTo(address: string): Promise<Summary[]> {
  const response = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${address}"`)}`);
  expect(response.ok).toBe(true);
  return (await response.json()).messages;
}

/**
 * The reset link in the newest mail to this address, as a path on the site the tests run against:
 * the mail names the stack's APP_BASE_URL, which PLAYWRIGHT_BASE_URL may differ from.
 */
export async function resetLinkTo(address: string): Promise<string> {
  let mails: Summary[] = [];
  await expect.poll(async () => (mails = await mailsTo(address)).length).toBeGreaterThan(0);
  const response = await fetch(`${MAILPIT}/api/v1/message/${mails[0].ID}`);
  const text: string = (await response.json()).Text;
  const link = text.match(/https?:\/\/\S+\/reset-password\?token=[A-Za-z0-9_-]+/);
  expect(link, "a reset link in the mail").not.toBeNull();
  const url = new URL(link![0]);
  return url.pathname + url.search;
}

/** Deletes every mail to this address, so the stack's Mailpit keeps only what people sent. */
export async function deleteMailsTo(address: string) {
  const ids = (await mailsTo(address)).map((mail) => mail.ID);
  if (ids.length > 0) {
    await fetch(`${MAILPIT}/api/v1/messages`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ IDs: ids }),
    });
  }
}
