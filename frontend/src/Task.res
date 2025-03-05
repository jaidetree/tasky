open Fetch

@spice
type session = {
  id: string,
  started_at: float,
  ended_at: option<float>,
  interrupted_by_task_id: option<string>,
  notes: string,
}

@spice
type rec task = {
  id: string,
  name: string,
  notes: string,
  parent_task_id: option<string>,
  estimated_time: int,
  time_sessions: array<session>,
  tasks: array<task>,
}

let createTask: task => promise<task> = async task => {
  let response = await fetch(
    "/api/tasks.json",
    {
      "body": task->task_encode,
      "method": "POST",
      "headers": {
        "Content-Type": "json",
      },
    },
  )
  let json = await response->Fetch.json
  let decoded = json->task_decode
  decoded->Result.getExn
}

let fetchTask: string => promise<task> = async taskId => {
  let response = await fetch(
    `/api/tasks/${taskId}.json`,
    {
      "method": "GET",
      "headers": {
        "Content-Type": "json",
      },
    },
  )
  let json = await response->Fetch.json
  let decoded = json->task_decode

  decoded->Result.getExn
}
