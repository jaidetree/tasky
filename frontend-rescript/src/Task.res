open Fetch

@spice
type session = {
  id: string,
  start_time: float,
  end_time: option<float>,
  notes: string,
  interrupted_by_task_id: option<string>,
}

@spice
type estimated_time_map = {
  hours: int,
  minutes: int,
}

@spice
type task = {
  id: string,
  title: string,
  notes: string,
  estimated_time: int,
  estimated_time_map: estimated_time_map,
  due_date: option<string>,
  completed_at: option<string>,
  created_at: string,
  updated_at: option<string>,
  parent_task_id: option<string>,
  tracked_time: int,
  time_sessions: array<session>,
}

@spice
type tasks = array<task>

@spice
type tasksList = {tasks: tasks}

@spice
type draft = {
  title: string,
  notes: string,
  estimated_time_map: estimated_time_map,
  due_date: option<string>,
  parent_task_id: option<string>,
}

@spice
type createRequest = {task: draft}

let createTask: draft => promise<task> = async task => {
  let body = {task: task}
  let response = await fetch(
    "/api/tasks",
    {
      "body": body->createRequest_encode->Js.Json.stringify,
      "method": "POST",
      "headers": {
        "Content-Type": "application/json",
      },
    },
  )
  let json = await response->Fetch.json
  let decoded = json->task_decode
  decoded->Result.getExn
}

let fetchTask: string => promise<task> = async taskId => {
  let response = await fetch(
    `/api/tasks/${taskId}`,
    {
      "method": "GET",
      "headers": {
        "Content-Type": "application/json",
      },
    },
  )
  let json = await response->Fetch.json
  let decoded = json->task_decode

  decoded->Result.getExn
}

let fetchAll: unit => promise<tasks> = async () => {
  let response = await fetch(
    `/api/tasks`,
    {
      "method": "GET",
      "headers": {
        "Content-Type": "application/json",
      },
    },
  )
  let json = await response->Fetch.json
  let decoded = json->tasksList_decode
  let {tasks} = decoded->Result.getExn

  tasks
}
