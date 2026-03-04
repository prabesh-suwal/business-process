import { useAccess } from '../context/AccessContext';

/**
 * useAccess hook - re-exported for convenience.
 * 
 * Usage:
 *   const { has, hasAny, hasModule, constraints } = useAccess();
 * 
 *   has("MMS.MEMO.CREATE")                              // exact permission check
 *   hasAny(["MMS.MEMO.APPROVE", "MMS.MEMO.REJECT"])     // any of these
 *   hasModule("MMS.REPORT")                              // any action in REPORT module
 *   constraints("MMS")                                   // { branchIds: [...] }
 */
export { useAccess };
export default useAccess;
