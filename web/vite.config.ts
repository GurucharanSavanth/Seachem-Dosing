import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

/**
 * Vite configuration for Seachem Dosing v2.0 web app.
 * Per ADR-004 — Vite + TypeScript + PWA stack.
 */
export default defineConfig({
  plugins: [
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Seachem Dosing Calculator',
        short_name: 'Seachem Dose',
        description: 'Aquarium dosing & water parameter analysis',
        theme_color: '#00695C',
        background_color: '#00695C',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icon-512.png', sizes: '512x512', type: 'image/png' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg}']
      }
    })
  ],
  build: {
    target: 'es2022',
    sourcemap: true,
    outDir: 'dist'
  },
  test: {
    // Default 'node' env — calculator tests are pure functions, no DOM needed.
    // UI tests added later (per ADR-004) will switch this to 'happy-dom'.
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov']
    }
  }
});
