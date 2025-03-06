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
    type action =
      | Init
      | Create
      | Update(task)
      | Save
      | Saved(task)
  }

  module App = {
    type action =
      | Init
      | NewTask(NewTask.action)
      | OpenTask(Task.action)
  }
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

  type transition = {
    prev: state,
    next: state,
    action: action,
  }

  let stateSignal: Signal.t<state> = Signal.make(Inactive)

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
          started_at: DateTime.now(),
          ended_at: None,
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
            ended_at: Some(DateTime.now()),
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
    | Active(task)
    | Saving(promise<task>)

  type action = Actions.NewTask.action

  type transition = {
    prev: state,
    next: state,
    action: action,
  }

  let stateSignal: Signal.t<state> = Signal.make(Inactive)

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState
    | (Inactive, Create) =>
      Active({
        id: "",
        name: "",
        notes: "",
        parent_task_id: None,
        estimated_time: 0,
        time_sessions: [],
        tasks: [],
      })
    | (Active(_task), Update(task)) => Active(task)
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

module AppFSM = {
  type state =
    | Idle
    | Task(TaskFSM.state)
    | NewTask(NewTaskFSM.state)

  type action = Actions.App.action

  type transition = {
    prev: state,
    next: state,
    action: action,
    created_at: float,
  }

  let stateSignal: Signal.t<state> = Signal.make(Idle)

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState

    // Task view
    | (Idle, OpenTask(action)) => Task(TaskFSM.reduce(Inactive, action))
    | (Task(_prevId), OpenTask(action)) => Task(TaskFSM.reduce(Inactive, action))

    // New Task View
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
    action: Init,
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

Signal.effect(() => {
  let transition = AppFSM.transitionSignal->Signal.get

  Js.Console.log2("transition", transition)

  None
})
