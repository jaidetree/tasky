open Preact
open Task

let actionSignal: Signal.t<option<'action>> = Signal.make(None)

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

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
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
  }

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

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
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
  }
}

module AppFSM = {
  type state =
    | Idle
    | Task(TaskFSM.state)
    | CreatingTask(NewTaskFSM.state)

  type action =
    | Init
    | OpenTask(string)
    | NewTask
    | NewTaskFSM(NewTaskFSM.action)
    | TaskFSM(TaskFSM.action)

  type transition = {
    prev: state,
    next: state,
    action: action,
    created_at: float,
  }

  let actionSignal: Signal.t<action> = Signal.make(Init)
  let stateSignal: Signal.t<state> = Signal.make(Idle)

  let reduce = (prevState: state, action: action): state => {
    switch (prevState, action) {
    | (_, Init) => prevState

    // Task view
    | (Idle, OpenTask(taskId)) => Task(TaskFSM.reduce(Inactive, Fetch(taskId)))
    | (Task(prevState), TaskFSM(action)) => Task(TaskFSM.reduce(prevState, action))
    | (Task(_prevId), OpenTask(nextId)) => Task(TaskFSM.reduce(Inactive, Fetch(nextId)))

    // New Task View
    | (_, NewTask) => CreatingTask(NewTaskFSM.reduce(Inactive, NewTask))
    | (CreatingTask(prevState), NewTaskFSM(action)) =>
      CreatingTask(NewTaskFSM.reduce(prevState, action))

    // Open task view while creating task
    | (CreatingTask(_), OpenTask(taskId)) => Task(TaskFSM.reduce(Inactive, Fetch(taskId)))

    // Corner cases
    | (_, NewTaskFSM(_)) => prevState
    | (_, TaskFSM(_)) => prevState
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
      stateSignal->Signal.set(nextState)
      transitionSignal->Signal.set({
        next: nextState,
        prev: prevState,
        action,
        created_at: DateTime.now(),
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
