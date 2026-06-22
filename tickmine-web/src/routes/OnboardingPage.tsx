import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';
import { TokenStep } from '@/components/onboarding/TokenStep';
import { Logo } from '@/components/layout/Logo';

export function OnboardingPage() {
  const navigate = useNavigate();
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated());
  const onboardingComplete = useSessionStore((s) => s.onboardingComplete);
  const completeOnboarding = useSessionStore((s) => s.completeOnboarding);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }
    if (onboardingComplete) navigate('/chat', { replace: true });
  }, [isAuthenticated, onboardingComplete, navigate]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[#f7f7f5] px-6">
      <div className="mb-8 text-center">
        <Logo className="mb-4 justify-center" />
        <p className="text-[14px] text-[#5c5c58]">最后一步：连接你的滴答清单</p>
      </div>
      <TokenStep
        onSuccess={() => {
          completeOnboarding();
          navigate('/chat');
        }}
      />
    </div>
  );
}
