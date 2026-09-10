import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Salida autocontenida para imagen Docker ligera (server.js + deps mínimas).
  output: "standalone",
};

export default nextConfig;
