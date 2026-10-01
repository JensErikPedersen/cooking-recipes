"use client";

import { useEffect, useState } from "react";

// Placeholder for Part 2: a render plus a browser-side fetch of /hello.
export default function Home() {
  const [message, setMessage] = useState("Loading...");

  useEffect(() => {
    fetch("/hello")
      .then((response) => response.json())
      .then((body) => setMessage(body.message));
  }, []);

  return (
    <>
      <h1 className="text-3xl font-bold">Cooking Recipes</h1>
      <p className="mt-4">{message}</p>
    </>
  );
}
