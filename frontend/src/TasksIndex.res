open Preact

module TaskRow = {
  @jsx.component
  let make = (~task: Task.task) => {
    <tr>
      <td> {task.title->string} </td>
      <td className="py-3 px-4 border-b"> {task.estimated_time->Int.toString->string} </td>
      <td className="py-3 px-4 border-b"> {task.tracked_time->Int.toString->string} </td>
    </tr>
  }
}

module TasksTable = {
  @jsx.component
  let make = (~tasks: Task.tasks) => {
    <div className="overflow-x-auto shadow-md rounded-lg">
      <table className="min-w-full table-auto">
        <thead className="bg-gray-100 dark:bg-slate-600">
          <tr>
            <th className="py-3 px-4 text-left"> {"Name"->string} </th>
            <th className="py-3 px-4 text-left"> {"Estimated Time"->string} </th>
            <th className="py-3 px-4 text-left"> {"Tracked Time"->string} </th>
            <th className="py-3 px-4 text-left"> {"Due Date"->string} </th>
            <th className="py-3 px-4 text-left"> {"Status"->string} </th>
          </tr>
        </thead>
        <tbody> {tasks->Array.map(task => <TaskRow task={task} key={task.id} />)->array} </tbody>
      </table>
    </div>
  }
}

@jsx.component
let make = (~tasks: Task.tasks) => {
  let onClick = _e => {
    State.AppFSM.dispatch(NewTask(Create))
  }

  <section id="tasks-container" className="space-y-4">
    <header className="flex flex-row justify-between items-end">
      <h1 className="text-2xl font-bold"> {"Tasks"->string} </h1>
      <div className="inline-flex flex-row gap-2 justify-end">
        <button onClick={onClick} className="btn bg-blue-500"> {"New Task"->string} </button>
      </div>
    </header>
    <TasksTable tasks={tasks} />
  </section>
}
