defmodule TaskyWeb.TaskJSON do
  alias Tasky.Tracking.Task

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
    %{data: data(task)}
  end

  defp data(%Task{} = task) do
    {hours, minutes} = Tasky.Tracking.estimated_time_hours_minutes(task)

    %{
      id: task.id,
      title: task.title,
      notes: task.notes,
      estimated_time: task.estimated_time,
      estimated_time_map: %{
        hours: hours,
        minutes: minutes,
      },
      due_date: task.due_date,
      completed_at: task.completed_at,
      created_at: task.inserted_at,
      updated_at: task.updated_at,
      parent_task_id: task.parent_task_id,
      tracked_time: Tasky.Tracking.get_total_minutes_from_task(task),
      time_sessions: render_time_sessions(task)
    }
  end

  defp render_time_sessions(%{time_sessions: %Ecto.Association.NotLoaded{}}), do: []

  defp render_time_sessions(%{time_sessions: sessions}) when not is_nil(sessions) do
    for session <- sessions do
      %{
        id: session.id,
        start_time: session.start_time,
        end_time: session.end_time,
        original_end_time: session.original_end_time,
        notes: session.notes,
        interrupted_by_task_id: session.interrupted_by_task_id
      }
    end
  end

  defp render_time_sessions(_), do: []
end
