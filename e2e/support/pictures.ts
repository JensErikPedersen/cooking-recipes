import type { Page } from "@playwright/test";
import { csrfHeader } from "./api";

// Pictures for the tests, drawn by the browser rather than committed: a canvas encoded as JPEG or
// PNG. The left half is red and the right half blue, as in the backend's own TestPictures.

/** A picture of the given size, encoded by the page's browser. */
export async function drawPicture(page: Page, width: number, height: number, type = "image/jpeg") {
  const base64 = await page.evaluate(
    ({ width, height, type }) => {
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const context = canvas.getContext("2d")!;
      context.fillStyle = "#c00";
      context.fillRect(0, 0, width / 2, height);
      context.fillStyle = "#00c";
      context.fillRect(width / 2, 0, width - width / 2, height);
      return canvas.toDataURL(type).split(",")[1];
    },
    { width, height, type },
  );
  return Buffer.from(base64, "base64");
}

/**
 * A valid JPEG of exactly `bytes` bytes: a small one with zeros after its end marker, which a
 * decoder never reads. So the size limit is tested with a file that is otherwise accepted.
 */
export async function pictureOfSize(page: Page, bytes: number) {
  const picture = await drawPicture(page, 400, 300);
  return Buffer.concat([picture, Buffer.alloc(bytes - picture.length)]);
}

/** Uploads through the API on the page's own session, as the form will. */
export async function uploadPicture(page: Page, file: Buffer, name: string, altText: string, fileName = "picture.jpg") {
  return page.request.post("/api/v1/pictures", {
    headers: await csrfHeader(page),
    multipart: { file: { name: fileName, mimeType: "image/jpeg", buffer: file }, name, altText },
  });
}

/** The pixel width of a JPEG, decoded by the page's browser. */
export async function pictureWidth(page: Page, jpeg: Buffer) {
  return page.evaluate(async (base64) => {
    const image = new Image();
    image.src = `data:image/jpeg;base64,${base64}`;
    await image.decode();
    return image.naturalWidth;
  }, jpeg.toString("base64"));
}
