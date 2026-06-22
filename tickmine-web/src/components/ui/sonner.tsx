import { Toaster as Sonner, type ToasterProps } from "sonner"

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      theme="light"
      position="top-center"
      toastOptions={{
        classNames: {
          toast:
            "border border-[#dcdcd8] bg-[#fafaf8] text-[#1c1c1a] text-[13px] shadow-none",
          description: "text-[#5c5c58]",
          actionButton: "bg-[#1c1c1a] text-[#f7f7f5]",
          cancelButton: "border border-[#dcdcd8] text-[#5c5c58]",
        },
      }}
      {...props}
    />
  )
}

export { Toaster }
