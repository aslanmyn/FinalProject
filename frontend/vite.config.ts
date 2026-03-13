import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.VITE_API_PROXY_TARGET || "http://localhost:8080";

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": {
          target: proxyTarget,
          changeOrigin: true
        },
        "/v3/api-docs": {
          target: proxyTarget,
          changeOrigin: true
        },
        "/swagger-ui": {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  };
});
