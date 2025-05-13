defmodule TaskyWeb.TaskJSON do
  alias Tasky.Tracking.Task
  alias TaskyWeb.TimeSessionJSON

  @doc """
  Renders a list of tasks.
  """
  def index(%{tasks: tasks}) do
    %{tasks: for(task <- tasks, do: data(task))}
  end

  @doc """
  Renders a single task.
  """
  def show(%{task: task}) do
    data(task)
  end

  defp data(%Task{} = task) do
    %{
      id: task.id,
      title: task.title,
      description: task.description,
      estimated_time: task.estimated_time,
      due_date: task.due_date,
      sort_order: task.sort_order,
      completed_at: task.completed_at,
      created_at: task.inserted_at,
      updated_at: task.updated_at,
      parent_task_id: task.parent_task_id,
      tracked_time: Tasky.Tracking.get_total_seconds_from_task(task),
      time_sessions: render_time_sessions(task)
    }
  end

  defp render_time_sessions(%{time_sessions: %Ecto.Association.NotLoaded{}}), do: []

  defp render_time_sessions(%{time_sessions: sessions}) when not is_nil(sessions) do
    for session <- sessions do
      TimeSessionJSON.show(%{time_session: session})
    end
  end

  defp render_time_sessions(_), do: []
end
