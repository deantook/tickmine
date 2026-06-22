import type { SubscriptionTier } from '@/api/types';

export function formatSubscriptionTierLabel(tier: SubscriptionTier): string {
  switch (tier) {
    case 'FREE':
      return '免费版';
    case 'VIP':
      return 'VIP';
    case 'SVIP':
      return 'SVIP';
    default:
      return tier;
  }
}

export function parseSubscriptionTier(value: string): SubscriptionTier | null {
  if (value === 'FREE' || value === 'VIP' || value === 'SVIP') {
    return value;
  }
  return null;
}
