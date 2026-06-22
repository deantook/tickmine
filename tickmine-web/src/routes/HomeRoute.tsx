import { Navigate } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';
import { HomePage } from '@/routes/HomePage';

export function HomeRoute() {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const onboardingComplete = useSessionStore((s) => s.onboardingComplete);

  if (isAuthenticated) {
    if (!onboardingComplete) return <Navigate to="/onboarding" replace />;
    return <Navigate to="/chat" replace />;
  }

  return <HomePage />;
}
