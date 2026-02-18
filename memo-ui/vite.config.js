import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(({ mode }) => {
    // Load env file based on current mode, looking for VITE_ variables
    const env = loadEnv(mode, process.cwd(), '');

    return {
        plugins: [react()],
        // This is the most important part for subpaths (/memo/)
        base: env.VITE_BASE_URL || '/',   // ← dynamic
        server: {
            port: 5176
        },
        resolve: {
            alias: {
                'ids': path.resolve(__dirname, 'src/shims/ids.js'),
            },
        },
        optimizeDeps: {
            include: [
                'dmn-js/lib/Modeler',
                'dmn-js-drd',
                'dmn-js-decision-table',
                'dmn-js-literal-expression',
            ],
        },
        build: {
            commonjsOptions: {
                transformMixedEsModules: true,
            },
        },
    }
})