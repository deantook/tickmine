import { useEffect } from 'react';
import { getMe } from '@/api/endpoints';
import { parseSubscriptionTier } from '@/lib/subscriptionTier';
import { useSessionStore } from '@/stores/sessionStore';

export function useSubscriptionTier() {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const tier = useSessionStore((s) => s.subscriptionTier);
  const setSubscriptionTier = useSessionStore((s) => s.setSubscriptionTier);

  useEffect(() => {
    if (!isAuthenticated) return;
    getMe()
      .then((r) => {
        const parsed = parseSubscriptionTier(r.subscriptionTier);
        if (parsed) setSubscriptionTier(parsed);
      })
      .catch(() => {});
  }, [isAuthenticated, setSubscriptionTier]);

  return tier;
}
