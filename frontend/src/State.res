open Preact
open Task

module TaskFSM = {
  type state =
    | Inactive
    | Fetching(promise<task>)
    | Error(string, string)
    | Idle(task)
    | Running(task, session)

  type action =
    | Init
    | Fetch(string)
    | Fetched(task)
    | ClockIn
    | ClockOut
    | UpdateTask(task)

  type transition = {
    prev: state,
    next: state,
    action: action,
  }

  let actionSignal: Signal.t<action> = Signal.make(Init)
  let stateSignal: Signal.t<state> = Signal.make(Inactive)

  let transitionSignal: Signal.t<transition> = Signal.computed(() => {
    let action = actionSignal->Signal.get
    let prevState = stateSignal->Signal.peek

    let nextState = switch (prevState, action) {
    | (_, Init) => prevState
    | (Inactive, Fetch(taskId)) => Fetching(Task.fetchTask(taskId))
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

    if prevState !== nextState {
      stateSignal->Signal.set(nextState)
    }

    {next: nextState, prev: prevState, action}
  })

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(action)
  }
}

module NewTaskFSM = {
  type state =
    | Inactive
    | Active(task)
    | Saving(promise<task>)

  type action =
    | Init
    | NewTask
    | Update(task)
    | Save
    | Saved(task)

  type transition = {
    prev: state,
    next: state,
    action: action,
  }

  let actionSignal: Signal.t<action> = Signal.make(Init)
  let stateSignal: Signal.t<state> = Signal.make(Inactive)

  let transitionSignal: Signal.t<transition> = Signal.computed(() => {
    let action = actionSignal->Signal.get
    let prevState = stateSignal->Signal.peek

    let nextState = switch (prevState, action) {
    | (_, Init) => prevState
    | (Inactive, NewTask) =>
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
            actionSignal->Signal.set(Saved(task))

            resolve(task)
          }),
        )
      }
    | (Saving(_), Saved(_)) => Inactive
    | (Active(_task), NewTask) => prevState
    | (Inactive, _) => prevState
    | (Active(_), Saved(_)) => prevState
    | (Saving(_), _) => prevState
    }

    if prevState !== nextState {
      stateSignal->Signal.set(nextState)
    }

    {next: nextState, prev: prevState, action}
  })

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(action)
  }
}

module AppFSM = {
  type state =
    | Idle
    | Task(string)
    | New

  // Separate into appAction and taskAction
  type action =
    | Init
    | OpenTask(string)
    | NewTask

  type transition = {
    prev: state,
    next: state,
    action: action,
  }

  let actionSignal: Signal.t<action> = Signal.make(Init)
  let stateSignal: Signal.t<state> = Signal.make(Idle)

  let transitionSignal: Signal.t<transition> = Signal.computed(() => {
    let action = actionSignal->Signal.get
    let prevState = stateSignal->Signal.peek

    let nextState = switch (prevState, action) {
    | (_, Init) => prevState
    | (Idle, OpenTask(task_id)) => Task(task_id)
    | (_, NewTask) => {
        NewTaskFSM.dispatch(NewTask)
        New
      }
    | (Task(_prevId), OpenTask(nextId)) => Task(nextId)
    | (New, OpenTask(task_id)) => Task(task_id)
    }

    if prevState !== nextState {
      stateSignal->Signal.set(nextState)
    }

    {next: nextState, prev: prevState, action}
  })

  let dispatch = (action: action): unit => {
    actionSignal->Signal.set(action)
  }
}
