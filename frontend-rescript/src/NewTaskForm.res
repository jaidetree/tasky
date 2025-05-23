open Preact
open State

module Update = {
  let title = (title: string) => {
    AppFSM.dispatch(NewTask(Update(Title(title))))
  }

  let estimatedTimeHours = (hours: int) => {
    AppFSM.dispatch(NewTask(Update(EstimateHours(hours))))
  }

  let estimatedTimeMinutes = (minutes: int) => {
    AppFSM.dispatch(NewTask(Update(EstimateMinutes(minutes))))
  }

  let dueDate = (dueDate: option<string>) => {
    AppFSM.dispatch(NewTask(Update(DueDate(dueDate))))
  }

  let parentTask = (id: option<string>) => {
    AppFSM.dispatch(NewTask(Update(ParentTask(id))))
  }

  let notes = (notes: string) => {
    AppFSM.dispatch(NewTask(Update(Notes(notes))))
  }
}

let onSubmit = e => {
  e->JsxEvent.Form.preventDefault

  AppFSM.dispatch(NewTask(Save))
}

let onInput = e => {
  let target = e->EventUtils.FormEvent.target
  let name = target->DomUtils.getName
  let value = target->EventUtils.getValue

  switch (name, value) {
  | (Some("title"), Some(title)) => Update.title(title)
  | (Some("estimated_time_hours"), Some(hours)) =>
    Update.estimatedTimeHours(hours->Int.fromString->Option.getOr(0))
  | (Some("estimated_time_minutes"), Some(minutes)) =>
    Update.estimatedTimeMinutes(minutes->Int.fromString->Option.getOr(0))
  | (Some("due_date"), Some(dueDate)) =>
    Update.dueDate(
      if dueDate === "" {
        None
      } else {
        Some(dueDate)
      },
    )
  | (Some("parent_task_id"), Some(dueDate)) =>
    Update.parentTask(
      if dueDate === "" {
        None
      } else {
        Some(dueDate)
      },
    )
  | (Some("notes"), Some(notes)) => Update.notes(notes)
  | (Some(field), None) => Js.Console.warn(`Could not get form value from field ${field}`)
  | (None, _) => Js.Console.warn(`Could not get name of field`)
  | (_, _) => ()
  }
}

let value = (opt: option<Task.draft>, fn: Task.draft => 'value) => opt->Option.mapOr("", fn)

@jsx.component
let make = (~state: NewTaskFSM.state) => {
  let formData = switch state {
  | Active(draft) => Some(draft)
  | _ => None
  }
  let estimatedTime = switch formData {
  | Some(draft) => draft.estimated_time_map
  | _ => {hours: 0, minutes: 20}
  }

  let tasks = TasksFSM.getTasks()

  <form onSubmit onInput className="gap-2">
    <header>
      <h2 className="text-xl"> {"New Task"->string} </h2>
    </header>
    <div className="flex flex-col gap-2">
      <section>
        <label htmlFor="id_title" className="block py-2"> {"Title"->string} </label>
        <input type_="text" name="title" className="bg-stone-700/20 p-2 rounded-sm w-full" />
      </section>
      <section>
        <label htmlFor="id_notes" className="block py-2"> {"Notes"->string} </label>
        <textarea
          name="notes"
          id="id_notes"
          className="bg-stone-700/20 p-2 rounded-sm w-full h-40"
          value={formData->value(draft => draft.notes)}
        />
      </section>
      <section>
        <label htmlFor="id_estimated_time" className="block py-2"> {"Estimate"->string} </label>
        <div className="flex flex-row gap-2 items-center">
          <div className="flex flex-col gap-2 items-center">
            <input
              type_="range"
              name="estimated_time_hours"
              min="0"
              max="23"
              step={1.0}
              className="block bg-stone-700/20 p-2 rounded-sm flex-shrink w-full"
              value={estimatedTime.hours->Int.toString}
            />
            <span>
              <output />
              {`${estimatedTime.hours->Int.toString} hrs`->string}
            </span>
          </div>
          <div className="flex flex-col gap-2 items-center">
            <input
              type_="range"
              name="estimated_time_minutes"
              min="0"
              max="59"
              className="block bg-stone-700/20 p-2 rounded-sm flex-shrink w-full"
              value={estimatedTime.minutes->Int.toString}
            />
            <span> {`${estimatedTime.minutes->Int.toString} min`->string} </span>
          </div>
        </div>
      </section>
      <section>
        <label htmlFor="id_due_date" className="block py-2"> {"Due Date"->string} </label>
        <input
          type_="date"
          name="due_date"
          className="bg-stone-700/20 p-2 rounded-sm w-full"
          value={formData->value(draft => draft.due_date->Option.getOr(""))}
        />
      </section>
      <section>
        <label htmlFor="id_parent_task_id" className="block py-2"> {"Parent Task"->string} </label>
        <select
          className="bg-stone-700/20 p-2 rounded-sm w-full"
          id="id_parent_task_id"
          name="parent_task_id"
          value={formData->value(draft => draft.parent_task_id->Option.getOr(""))}>
          <option value=""> {"-- No Parent Task --"->string} </option>
          {tasks
          ->Array.map(task => {
            <option key={task.id} value={task.id}> {task.title->string} </option>
          })
          ->array}
        </select>
      </section>
      <section className="flex flex-row justify-end items-center gap-2 py-4">
        <button type_="submit" className="btn py-2 px-4 text-white bg-blue-500">
          {"Create"->string}
        </button>
      </section>
    </div>
  </form>
}
