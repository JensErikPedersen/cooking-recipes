import type { Metadata, Viewport } from "next";
import { Fira_Sans, Young_Serif } from "next/font/google";
import "./globals.css";

// Young Serif for names and headings, Fira Sans for everything else (docs/PLAN_V7.md, "Design
// language"). next/font serves both from this origin.
const youngSerif = Young_Serif({
  variable: "--font-young-serif",
  subsets: ["latin"],
  weight: "400",
});

const firaSans = Fira_Sans({
  variable: "--font-fira-sans",
  subsets: ["latin"],
  weight: ["400", "600"],
  style: ["normal", "italic"],
});

export const metadata: Metadata = {
  title: "Cooking Recipes",
  description: "Recipes, categories and ingredients",
};

// Olive colours a phone browser's bar, as the header band below it.
export const viewport: Viewport = {
  themeColor: "#4f5b2a",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${youngSerif.variable} ${firaSans.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">{children}</body>
    </html>
  );
}
