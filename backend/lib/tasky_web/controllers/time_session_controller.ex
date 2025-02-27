defmodule TaskyWeb.TimeSessionController do
  use TaskyWeb, :controller

  alias Tasky.Tracking
  alias Tasky.Tracking.TimeSession

  action_fallback TaskyWeb.FallbackController

  def index(conn, _params) do
    time_sessions = Tracking.list_time_sessions()
    render(conn, :index, time_sessions: time_sessions)
  end

  def create(conn, %{"time_session" => time_session_params}) do
    with {:ok, %TimeSession{} = time_session} <- Tracking.create_time_session(time_session_params) do
      conn
      |> put_status(:created)
      |> put_resp_header("location", ~p"/api/time_sessions/#{time_session}")
      |> render(:show, time_session: time_session)
    end
  end

  def show(conn, %{"id" => id}) do
    time_session = Tracking.get_time_session!(id)
    render(conn, :show, time_session: time_session)
  end

  def update(conn, %{"id" => id, "time_session" => time_session_params}) do
    time_session = Tracking.get_time_session!(id)

    with {:ok, %TimeSession{} = time_session} <-
           Tracking.update_time_session(time_session, time_session_params) do
      render(conn, :show, time_session: time_session)
    end
  end

  def delete(conn, %{"id" => id}) do
    time_session = Tracking.get_time_session!(id)

    with {:ok, %TimeSession{}} <- Tracking.delete_time_session(time_session) do
      send_resp(conn, :no_content, "")
    end
  end

  def end_session(conn, %{"id" => id}) do
    time_session = Tracking.get_time_session!(id)

    with {:ok, %TimeSession{} = time_session} <- Tracking.end_time_session(time_session) do
      render(conn, :show, time_session: time_session)
    end
  end

  def interrupt(conn, %{"id" => id, "interrupted_by_task_id" => interrupted_by_task_id}) do
    time_session = Tracking.get_time_session!(id)

    with {:ok, %TimeSession{} = time_session} <-
           Tracking.interrupt_time_session(time_session, interrupted_by_task_id) do
      render(conn, :show, time_session: time_session)
    end
  end

  def active(conn, _params) do
    case Tracking.get_active_time_session() do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(%{data: nil, message: "No active time session found"})

      time_session ->
        render(conn, :show, time_session: time_session)
    end
  end
end
