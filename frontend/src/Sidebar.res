open Preact
open State
open CSSUtils

@jsx.component
let make = () => {
  let state = AppFSM.stateSignal->Signal.get
  let isActive = switch state {
  | Task(_)
  | NewTask(_) => true
  | _ => false
  }

  <div
    className={classNames([
      "bg-gray-200 dark:bg-slate-700/20 overflow-hidden transition-all duration-500",
      isActive ? "w-[30rem]" : "w-0",
    ])}>
    <div className="w-[30rem] p-4">
      {switch state {
      | Idle => Preact.null
      | NewTask(state) => <NewTaskForm state={state} />
      | Task(_taskState) => Preact.null
      }}
    </div>
  </div>
}
