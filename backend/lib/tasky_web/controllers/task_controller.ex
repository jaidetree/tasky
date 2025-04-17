defmodule TaskyWeb.TaskController do
  use TaskyWeb, :controller

  alias Tasky.Tracking
  alias Tasky.Tracking.Task

  action_fallback TaskyWeb.FallbackController

  def index(conn, _params) do
    tasks = Tracking.list_tasks_with_time_sessions()
    render(conn, :index, tasks: tasks)
  end

  def create(conn, %{"task" => task_params}) do
    task_params = Tracking.normalize_estimated_time(task_params)

    IO.inspect(task_params)

    with {:ok, %Task{} = task} <- Tracking.create_task(task_params) do
      conn
      |> put_status(:created)
      |> put_resp_header("location", ~p"/api/tasks/#{task}")
      |> render(:show, task: task)
    end
  end

  def show(conn, %{"id" => id}) do
    task = Tracking.get_task_with_sessions!(id)
    render(conn, :show, task: task)
  end

  def update(conn, %{"id" => id, "task" => task_params}) do
    task = Tracking.get_task_with_sessions!(id)

    with {:ok, %Task{} = task} <- Tracking.update_task(task, task_params) do
      render(conn, :show, task: task)
    end
  end

  def delete(conn, %{"id" => id}) do
    task = Tracking.get_task!(id)

    with {:ok, %Task{}} <- Tracking.delete_task(task) do
      send_resp(conn, :no_content, "")
    end
  end

  def complete(conn, %{"task_id" => id}) do
    task = Tracking.get_task!(id)

    with {:ok, %Task{} = task} <- Tracking.complete_task(task) do
      render(conn, :show, task: task)
    end
  end

  def incomplete(conn, %{"task_id" => id}) do
    task = Tracking.get_task!(id)

    with {:ok, %Task{} = task} <- Tracking.incomplete_task(task) do
      render(conn, :show, task: task)
    end
  end
end
