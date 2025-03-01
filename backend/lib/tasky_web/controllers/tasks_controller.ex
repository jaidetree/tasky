defmodule TaskyWeb.TasksController do
  use TaskyWeb, :controller

  alias Tasky.Tracking

  def index(conn, _params) do
    tasks = Tracking.list_tasks_with_time_sessions()
    render(conn, :index, tasks: tasks)
  end
end
