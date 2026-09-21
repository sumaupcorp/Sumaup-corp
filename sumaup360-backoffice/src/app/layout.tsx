import type { Metadata } from "next";
import { Manrope, Sora } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";

const manrope = Manrope({ variable: "--font-manrope", subsets: ["latin"], display: "swap" });
const sora = Sora({ variable: "--font-sora", subsets: ["latin"], display: "swap" });

export const metadata: Metadata = {
  title: { default: "SUMAUP360 · Backoffice", template: "%s · Backoffice" },
  description: "Plataforma interna de SUMAUP360 para el staff (soporte, comprobantes y licencias).",
  applicationName: "SUMAUP360",
  keywords: ["SUMAUP360", "Backoffice", "staff", "SUNAT", "comprobantes", "licencias"],
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={`${manrope.variable} ${sora.variable} h-full antialiased`}>
      <body className="min-h-full bg-white text-foreground">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
