import { Navigate } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';

export function RootRedirect() {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const onboardingComplete = useSessionStore((s) => s.onboardingComplete);

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!onboardingComplete) return <Navigate to="/onboarding" replace />;
  return <Navigate to="/chat" replace />;
}
