open Preact

type toastStatus =
  | Success
  | Error
  | Info

type t = {
  title: string,
  message: string,
  duration: int,
  status: toastStatus,
}

@jsx.component
let make = (~toast: t) => {
  let color = switch toast.status {
  | Info => "bg-sky-400"
  | Error => "bg-rose-400"
  | Success => "bg-emerald-400"
  }

  <div className="bg-gray-800 rounded-lg p-4 relative shadow-md">
    <div className={CSSUtils.classNames(["absolute left-0 top-0 bottom-0 w-[3px]", color])} />
    <button>
      <HeroIcons.XIcon className="size-4" />
    </button>
    <h1> {toast.title->string} </h1>
    <p> {toast.message->string} </p>
  </div>
}
