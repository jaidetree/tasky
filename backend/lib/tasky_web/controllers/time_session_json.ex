defmodule TaskyWeb.TimeSessionJSON do
  alias Tasky.Tracking.TimeSession

  @doc """
  Renders a list of time_sessions.
  """
  def index(%{time_sessions: time_sessions}) do
    %{data: for(time_session <- time_sessions, do: data(time_session))}
  end

  @doc """
  Renders a single time_session.
  """
  def show(%{time_session: time_session}) do
    %{data: data(time_session)}
  end

  defp data(%TimeSession{} = time_session) do
    %{
      id: time_session.id,
      task_id: time_session.task_id,
      start_time: time_session.start_time,
      end_time: time_session.end_time,
      original_end_time: time_session.original_end_time,
      description: time_session.description,
      interrupted_by_task_id: time_session.interrupted_by_task_id,
      duration_minutes: TimeSession.duration_minutes(time_session),
      created_at: time_session.inserted_at,
      updated_at: time_session.updated_at
    }
  end
end
