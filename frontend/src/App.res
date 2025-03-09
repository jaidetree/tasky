open Preact
open State

@jsx.component
let make = () => {
  let state = TasksFSM.stateSignal->Signal.get

  useEffect0(() => {
    TasksFSM.dispatch(Fetch)
    None
  })

  <div>
    {switch state {
    | Empty => "Tasky"->Preact.string
    | Loading(_) => <div> {"Loading..."->Preact.string} </div>
    | Tasks({tasks}) =>
      <main className="flex flex-row items-stretch min-h-screen">
        <div className="max-w-4xl flex-grow mx-auto px-4 py-20 overflow-auto">
          <TasksIndex tasks={tasks} />
        </div>
        <Sidebar />
      </main>
    }}
  </div>
}
