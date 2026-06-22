import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';
import { RootRedirect } from '@/routes/RootRedirect';
import { LoginPage } from '@/routes/LoginPage';
import { RegisterPage } from '@/routes/RegisterPage';
import { OnboardingPage } from '@/routes/OnboardingPage';
import { ChatPage } from '@/routes/ChatPage';
import { SettingsPage } from '@/routes/SettingsPage';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function RequireOnboarding({ children }: { children: React.ReactNode }) {
  const done = useSessionStore((s) => s.onboardingComplete);
  if (!done) return <Navigate to="/onboarding" replace />;
  return <>{children}</>;
}

function GuestOnly({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const onboardingComplete = useSessionStore((s) => s.onboardingComplete);
  if (isAuthenticated) {
    return <Navigate to={onboardingComplete ? '/chat' : '/onboarding'} replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route
          path="/login"
          element={
            <GuestOnly>
              <LoginPage />
            </GuestOnly>
          }
        />
        <Route
          path="/register"
          element={
            <GuestOnly>
              <RegisterPage />
            </GuestOnly>
          }
        />
        <Route
          path="/onboarding"
          element={
            <RequireAuth>
              <OnboardingPage />
            </RequireAuth>
          }
        />
        <Route
          path="/chat"
          element={
            <RequireAuth>
              <RequireOnboarding>
                <ChatPage />
              </RequireOnboarding>
            </RequireAuth>
          }
        />
        <Route
          path="/settings"
          element={
            <RequireAuth>
              <SettingsPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<RootRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}
