import { useEffect, useState } from 'react';
import { getTokenStatus } from '@/api/endpoints';
import { useSessionStore } from '@/stores/sessionStore';

export function useTokenStatus() {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const [connected, setConnected] = useState<boolean | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      setConnected(null);
      return;
    }
    getTokenStatus()
      .then((r) => setConnected(r.connected))
      .catch(() => setConnected(false));
  }, [isAuthenticated]);

  return connected;
}
