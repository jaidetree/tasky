open Preact
open Task

module Actions = {
  module Task = {
    type action =
      | Init
      | Open(string)
      | Fetched(task)
      | ClockIn
      | ClockOut
      | UpdateTask(task)
  }

  module NewTask = {
    type field =
      | Title(string)
      | EstimateHours(int)
      | EstimateMinutes(int)
      | Notes(string)
      | ParentTask(option<string>)
      | DueDate(option<string>)

    type action =
      | Init
      | Create
      | Update(field)
      | Save
      | Saved(task)
  }

  module Tasks = {
    type action =
      | Init
      | Fetch
      | Fetched(array<task>)
  }

  module App = {
    type action =
      | Init
      | NewTask(NewTask.action)
      | OpenTask(Task.action)
  }
}

type transition<'state, 'action> = {
  prev: 'state,
  next: 'state,
  action: 'action,
  created_at: float,
}

let actionSignal: Signal.t<Actions.App.action> = Signal.make(Actions.App.Init)

module TaskFSM = {
  type state =
    | Inactive
    | Fetching(promise<task>)
    | Error(string, string)
    | Idle(task)
    | Running(task, session)

  type action = Actions.Task.action

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState
    | (Inactive, Open(taskId)) => Fetching(Task.fetchTask(taskId))
    | (Fetching(_), Fetched(task)) => Idle(task)
    | (Idle(task), ClockIn) =>
      Running(
        task,
        {
          id: "",
          start_time: DateTime.now(),
          end_time: None,
          interrupted_by_task_id: None,
          notes: "",
        },
      )
    | (Running(task, session), ClockOut) =>
      Idle({
        ...task,
        time_sessions: [
          ...task.time_sessions,
          {
            ...session,
            end_time: Some(DateTime.now()),
          },
        ],
      })

    | (Idle(_), UpdateTask(nextTask)) => Idle(nextTask)
    | (Running(_, session), UpdateTask(nextTask)) => Running(nextTask, session)
    | (Running(_, _), ClockIn) => prevState
    | (_, Fetched(_)) => prevState
    | (Inactive, _) => prevState
    | (_, _) => prevState
    }
  }

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(Actions.App.OpenTask(action))
  }
}

module NewTaskFSM = {
  type state =
    | Inactive
    | Active(draft)
    | Saving(promise<task>)

  type action = Actions.NewTask.action

  module Reducers = {
    let title = (draft: draft, title: string) => {
      ...draft,
      title,
    }

    let estimatedTimeHours = (draft: draft, hours: int) => {
      ...draft,
      estimated_time_map: {
        ...draft.estimated_time_map,
        hours,
      },
    }

    let estimatedTimeMinutes = (draft: draft, minutes: int) => {
      ...draft,
      estimated_time_map: {
        ...draft.estimated_time_map,
        minutes,
      },
    }

    let dueDate = (draft: draft, due_date: option<string>) => {
      ...draft,
      due_date,
    }

    let parentTask = (draft: draft, parent_task_id: option<string>) => {
      ...draft,
      parent_task_id,
    }

    let notes = (draft: draft, notes: string) => {
      ...draft,
      notes,
    }
  }

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState
    | (Inactive, Create) =>
      Active({
        title: "",
        notes: "",
        parent_task_id: None,
        estimated_time_map: {
          hours: 0,
          minutes: 20,
        },
        due_date: None,
      })
    | (Active(draft), Update(field)) =>
      Active(
        switch field {
        | Title(title) => Reducers.title(draft, title)
        | EstimateHours(hours) => Reducers.estimatedTimeHours(draft, hours)
        | EstimateMinutes(minutes) => Reducers.estimatedTimeMinutes(draft, minutes)
        | DueDate(dueDate) => Reducers.dueDate(draft, dueDate)
        | ParentTask(id) => Reducers.parentTask(draft, id)
        | Notes(notes) => Reducers.notes(draft, notes)
        },
      )
    | (Active(task), Save) => {
        open Promise
        let promise = createTask(task)

        Saving(
          promise->then(task => {
            actionSignal->Signal.set(Actions.App.NewTask(Actions.NewTask.Saved(task)))

            resolve(task)
          }),
        )
      }
    | (Saving(_), Saved(_)) => Inactive
    | (Active(_task), Create) => prevState
    | (Inactive, _) => prevState
    | (Active(_), Saved(_)) => prevState
    | (Saving(_), _) => prevState
    }
  }
}

module TasksFSM = {
  type context = {tasks: array<task>}

  type state =
    | Empty
    | Loading(promise<array<task>>)
    | Tasks(context)

  type action = Actions.Tasks.action
  type transition = transition<state, action>

  let actionSignal: Signal.t<action> = Signal.make(Actions.Tasks.Init)

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (Empty, Fetch) => {
        open Promise
        let promise = fetchAll()

        Loading(
          promise->then(tasks => {
            actionSignal->Signal.set(Fetched(tasks))

            resolve(tasks)
          }),
        )
      }
    | (Loading(_), Fetched(tasks)) => Tasks({tasks: tasks})
    | _ => prevState
    }
  }

  let stateSignal: Signal.t<state> = Signal.make(Empty)

  let transitionSignal: Signal.t<transition> = Signal.make({
    next: Empty,
    prev: Empty,
    action: Actions.Tasks.Init,
    created_at: DateTime.now(),
  })

  Signal.effect(() => {
    let action = actionSignal->Signal.get
    let prevState = stateSignal->Signal.peek
    let nextState = reduce(prevState, action)

    if prevState !== nextState {
      Signal.batch(() => {
        stateSignal->Signal.set(nextState)
        transitionSignal->Signal.set({
          next: nextState,
          prev: prevState,
          action,
          created_at: DateTime.now(),
        })
      })
    }

    None
  })

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(action)
  }

  let getTasks = () => {
    let state = stateSignal->Signal.get

    switch state {
    | Tasks(context) => context.tasks
    | _ => []
    }
  }
}

module AppFSM = {
  type state =
    | Idle
    | Task(TaskFSM.state)
    | NewTask(NewTaskFSM.state)

  type action = Actions.App.action

  type transition = transition<state, action>

  let stateSignal: Signal.t<state> = Signal.make(Idle)

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState

    // Task view
    | (Idle, OpenTask(action)) => Task(TaskFSM.reduce(Inactive, action))
    | (Task(_prevId), OpenTask(action)) => Task(TaskFSM.reduce(Inactive, action))

    // New Task View
    | (NewTask(state), NewTask(action)) => NewTask(NewTaskFSM.reduce(state, action))
    | (_, NewTask(action)) => NewTask(NewTaskFSM.reduce(Inactive, action))

    // Open task view while creating task
    | (NewTask(_), OpenTask(action)) => Task(TaskFSM.reduce(TaskFSM.Inactive, action))

    // Corner cases
    // _ => prevState
    }
  }

  let transitionSignal: Signal.t<transition> = Signal.make({
    next: Idle,
    prev: Idle,
    action: Actions.App.Init,
    created_at: DateTime.now(),
  })

  Signal.effect(() => {
    let action = actionSignal->Signal.get
    let prevState = stateSignal->Signal.peek
    let nextState = reduce(prevState, action)

    if prevState !== nextState {
      Signal.batch(() => {
        stateSignal->Signal.set(nextState)
        transitionSignal->Signal.set({
          next: nextState,
          prev: prevState,
          action,
          created_at: DateTime.now(),
        })
      })
    }

    None
  })

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(action)
  }
}

module Serialize = {
  @spice
  type newTaskState =
    | Init
    | Active(draft)

  @spice
  type taskState =
    | Init
    | Selected(string)

  @spice
  type t =
    | Idle
    | Task(taskState)
    | NewTask(newTaskState)
}

Signal.effect(() => {
  let transition = AppFSM.transitionSignal->Signal.get

  switch transition.next {
  | NewTask(state) =>
    switch state {
    | Active(task) => {
        let data: Serialize.t = NewTask(Active(task))
        let encoded = data->Serialize.t_encode
        let json = encoded->Js.Json.stringify
        Js.Console.log2("json", json)
      }
    | _ => ()
    }
  | _ => ()
  }
  Js.Console.log2("transition", transition)

  None
})
