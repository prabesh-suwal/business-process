// Shim for `ids` package to fix ESM named export issue.
// dmn-js-drd imports `{ Ids } from 'ids'` but ids only has a default export.
// We re-export the default as a named export.

// Use the CJS main entry which Vite can handle properly
import IdsModule from '../../node_modules/ids/dist/index.esm.js';
const Ids = IdsModule;
export { Ids };
export default Ids;
