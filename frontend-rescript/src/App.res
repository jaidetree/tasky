open Preact
open State

@jsx.component
let make = () => {
  let state = TasksFSM.stateSignal->Signal.get

  useEffect0(() => {
    TasksFSM.dispatch(Fetch)
    ToasterFSM.dispatch(
      Toast({
        title: "Test",
        status: Info,
        message: "Test toast message",
        duration: 0,
      }),
    )

    // AppFSM.dispatch(NewTask(Create))
    None
  })

  <div className="flex flex-row items-stretch min-h-screen">
    {switch state {
    | Empty => "Tasky"->Preact.string
    | Loading(_) => <div> {"Loading..."->Preact.string} </div>
    | Tasks({tasks}) =>
      <main className="max-w-4xl flex-grow mx-auto px-4 py-20 overflow-auto">
        <TasksIndex tasks={tasks} />
      </main>
    }}
    <Sidebar />
  </div>
}
