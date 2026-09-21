import type { Metadata } from "next";
import { Manrope, Sora } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";

const manrope = Manrope({ variable: "--font-manrope", subsets: ["latin"], display: "swap" });
const sora = Sora({ variable: "--font-sora", subsets: ["latin"], display: "swap" });

export const metadata: Metadata = {
  title: { default: "SUMAUP360 · Panel", template: "%s · SUMAUP360" },
  description:
    "Panel del SaaS ERP de SUMAUP360: ventas, caja POS, inventario, facturacion electronica (SUNAT) y contabilidad para tu negocio en Peru.",
  applicationName: "SUMAUP360",
  keywords: [
    "SUMAUP360", "ERP Peru", "punto de venta", "POS", "facturacion electronica",
    "SUNAT", "NubeFact", "inventario", "contabilidad", "CONCAR",
  ],
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
