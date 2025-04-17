open Preact
open State

module rec TaskRow: {
  @jsx.component
  let make: (~task: Task.task, ~tasks: Task.tasks, ~level: int=?) => element
} = {
  @jsx.component
  let make = (~task: Task.task, ~tasks: Task.tasks, ~level: int=0) => {
    let subTasks = tasks->Array.filter(subTask =>
      switch subTask.parent_task_id {
      | Some(id) => id === task.id
      | None => false
      }
    )
    let completed = switch task.completed_at {
    | Some(_date) => true
    | None => false
    }

    let onClick = _e => {
      AppFSM.dispatch(OpenTask(Open(task.id)))
    }

    <>
      <tr onClick className="border-b border-gray-500">
        <td className="py-3 px-4 text-left">
          <span style={{paddingLeft: level->Int.toString ++ "rem"}}> {task.title->string} </span>
        </td>
        <td className="py-3 px-4"> {task.due_date->Option.getOr("-")->string} </td>
        <td className="py-3 px-4"> {task.estimated_time->Int.toString->string} </td>
        <td className="py-3 px-4"> {task.tracked_time->Int.toString->string} </td>
        <td className="py-3 px-4">
          <form>
            <input type_="checkbox" checked={completed} name="complete" />
          </form>
        </td>
      </tr>
      {subTasks
      ->Array.map(task => <TaskRow task={task} key={task.id} tasks={tasks} level={level + 1} />)
      ->array}
    </>
  }
}

module TasksTable = {
  @jsx.component
  let make = (~tasks: Task.tasks) => {
    let rootTasks = tasks->Array.filter(task => {
      task.parent_task_id->Option.isNone
    })

    <div className="overflow-x-auto shadow-md rounded-lg">
      <table className="min-w-full table-auto">
        <thead className="bg-gray-100 dark:bg-slate-600">
          <tr>
            <th className="py-3 px-4 text-left"> {"Name"->string} </th>
            <th className="py-3 px-4 text-left"> {"Due"->string} </th>
            <th className="py-3 px-4 text-left"> {"Estimate"->string} </th>
            <th className="py-3 px-4 text-left"> {"Elapsed"->string} </th>
            <th className="py-3 px-4 text-left"> {""->string} </th>
          </tr>
        </thead>
        <tbody>
          {rootTasks->Array.map(task => <TaskRow task={task} key={task.id} tasks={tasks} />)->array}
        </tbody>
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
