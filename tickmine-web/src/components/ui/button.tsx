import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { Slot } from "radix-ui"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 text-[14px] font-medium whitespace-nowrap transition-colors outline-none disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default:
          "bg-[#1c1c1a] text-[#f7f7f5] hover:bg-[#333]",
        destructive:
          "bg-[#8b4513] text-[#fafaf8] hover:bg-[#6d3610]",
        outline:
          "border border-[#dcdcd8] bg-white/60 text-[#5c5c58] hover:border-[#aaa] hover:bg-[#f0f0ec]",
        secondary:
          "border border-[#dcdcd8] bg-white/60 text-[#5c5c58] hover:border-[#aaa]",
        ghost:
          "text-[12px] text-[#aaa] hover:text-[#5c5c58]",
        link:
          "text-[#1c1c1a] underline underline-offset-2 hover:text-[#555]",
      },
      size: {
        default: "px-5 py-2.5",
        xs: "px-2 py-1 text-[12px]",
        sm: "px-4 py-1.5 text-[13px]",
        lg: "px-6 py-3 text-[15px]",
        icon: "size-9 p-0",
        "icon-xs": "size-6 p-0 [&_svg:not([class*='size-'])]:size-3",
        "icon-sm": "size-8 p-0",
        "icon-lg": "size-10 p-0",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
