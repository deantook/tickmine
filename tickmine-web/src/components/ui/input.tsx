import * as React from "react"

import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn("field-input placeholder:text-[#aaa] disabled:opacity-50", className)}
      {...props}
    />
  )
}

export { Input }
