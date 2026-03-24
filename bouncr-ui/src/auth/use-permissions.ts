import { useContext } from 'react';
import { PermissionContext } from './permission-context';

export function usePermissions() {
  const ctx = useContext(PermissionContext);
  if (!ctx) throw new Error('usePermissions must be used within PermissionProvider');
  return ctx;
}
