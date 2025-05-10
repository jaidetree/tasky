defmodule Tasky.Tracking do
  @moduledoc """
  The Tracking context.
  """

  import Ecto.Query, warn: false
  alias Tasky.Repo

  alias Tasky.Tracking.Task

  @doc """
  Returns the list of tasks.

  ## Examples

      iex> list_tasks()
      [%Task{}, ...]

  """
  def list_tasks do
    Task
    |> where([t], is_nil(t.deleted_at))
    |> Repo.all()
  end

  @doc """
  Returns tasks with time sessions

  ## Examples

      iex> list_tasks_with_time_sessions()
      [%Task{}, ...]

  """
  def list_tasks_with_time_sessions do
    Task
    |> where([t], is_nil(t.deleted_at))
    |> preload(:time_sessions)
    |> Repo.all()
  end

  @doc """
  Gets a single task.

  Raises `Ecto.NoResultsError` if the Task does not exist.

  ## Examples

      iex> get_task!(123)
      %Task{}

      iex> get_task!(456)
      ** (Ecto.NoResultsError)

  """
  def get_task!(id), do: Repo.get!(Task, id)

  @doc """
  Gets a single task with sessions

  Raises `Ecto.NoResultsError` if the Task does not exist.

  ## Examples

      iex> get_task_with_sessions!(123)
      %Task{}

      iex> get_task_with_sessions!(456)
      ** (Ecto.NoResultsError)

  """
  def get_task_with_sessions!(id) do
    Task
    |> Repo.get!(id)
    |> Repo.preload(:time_sessions)
  end

  @doc """
  Creates a task.

  ## Examples

      iex> create_task(%{field: value})
      {:ok, %Task{}}

      iex> create_task(%{field: bad_value})
      {:error, %Ecto.Changeset{}}

  """
  def create_task(attrs \\ %{}) do
    %Task{}
    |> Task.changeset(attrs)
    |> Repo.insert()
  end

  @doc """
  Updates a task.

  ## Examples

      iex> update_task(task, %{field: new_value})
      {:ok, %Task{}}

      iex> update_task(task, %{field: bad_value})
      {:error, %Ecto.Changeset{}}

  """
  def update_task(%Task{} = task, attrs) do
    task
    |> Task.changeset(attrs)
    |> Repo.update()
  end

  @doc """
  Deletes a task.

  ## Examples

      iex> delete_task(task)
      {:ok, %Task{}}

      iex> delete_task(task)
      {:error, %Ecto.Changeset{}}

  """
  def delete_task(%Task{} = task) do
    task
    |> Task.soft_delete()
    |> Repo.update()
  end

  @doc """
  Returns an `%Ecto.Changeset{}` for tracking task changes.

  ## Examples

      iex> change_task(task)
      %Ecto.Changeset{data: %Task{}}

  """
  def change_task(%Task{} = task, attrs \\ %{}) do
    Task.changeset(task, attrs)
  end

  @doc """
  Marks a task as completed

  ## Examples

      iex> complete_task(task)
      {:ok, %Task{}}

  """
  def complete_task(%Task{} = task) do
    task
    |> Task.complete()
    |> Repo.update()
  end

  @doc """
  Marks a task as incomplete

  ## Examples

      iex> incomplete_task(task)
      {:ok, %Task{}}
  """
  def incomplete_task(%Task{} = task) do
    task
    |> Task.incomplete()
    |> Repo.update()
  end

  alias Tasky.Tracking.TimeSession

  @doc """
  Returns the list of time_sessions.

  ## Examples

      iex> list_time_sessions()
      [%TimeSession{}, ...]

  """
  def list_time_sessions do
    Repo.all(TimeSession)
  end

  @doc """
  Gets a single time_session.

  Raises `Ecto.NoResultsError` if the Time session does not exist.

  ## Examples

      iex> get_time_session!(123)
      %TimeSession{}

      iex> get_time_session!(456)
      ** (Ecto.NoResultsError)

  """
  def get_time_session!(id), do: Repo.get!(TimeSession, id)

  @doc """
  Creates a time_session.

  ## Examples

      iex> create_time_session(%{field: value})
      {:ok, %TimeSession{}}

      iex> create_time_session(%{field: bad_value})
      {:error, %Ecto.Changeset{}}

  """
  def create_time_session(attrs \\ %{}) do
    # Find any active time session that needs to be interrupted
    active_session = get_active_time_session()

    # Start a transaction to ensure both operations succeed or fail together
    Repo.transaction(fn ->
      # If there's an active session, interrupt it with the new task
      if active_session do
        {:ok, _interrupted} = interrupt_time_session(active_session, attrs.task_id)
      end

      # Create the new time session
      %TimeSession{}
      |> TimeSession.changeset(attrs)
      |> Repo.insert()
      |> case do
        {:ok, session} -> session
        {:error, changeset} -> Repo.rollback(changeset)
      end
    end)
  end

  @doc """
  Updates a time_session.

  ## Examples

      iex> update_time_session(time_session, %{field: new_value})
      {:ok, %TimeSession{}}

      iex> update_time_session(time_session, %{field: bad_value})
      {:error, %Ecto.Changeset{}}

  """
  def update_time_session(%TimeSession{} = time_session, attrs) do
    time_session
    |> TimeSession.changeset(attrs)
    |> Repo.update()
  end

  @doc """
  Deletes a time_session.

  ## Examples

      iex> delete_time_session(time_session)
      {:ok, %TimeSession{}}

      iex> delete_time_session(time_session)
      {:error, %Ecto.Changeset{}}

  """
  def delete_time_session(%TimeSession{} = time_session) do
    Repo.delete(time_session)
  end

  @doc """
  Returns an `%Ecto.Changeset{}` for tracking time_session changes.

  ## Examples

      iex> change_time_session(time_session)
      %Ecto.Changeset{data: %TimeSession{}}

  """
  def change_time_session(%TimeSession{} = time_session, attrs \\ %{}) do
    TimeSession.changeset(time_session, attrs)
  end

  @doc """
  Returns a new time session

  ## Examples

      iex> start_time_session(123)
      {:ok, %TimeSession{}}

      iex> start_time_session(456)
      {:error, %Ecto.Changeset{}}
  """
  def start_time_session(task_id) do
    attrs = %{
      task_id: task_id,
      start_time: DateTime.utc_now()
    }

    create_time_session(attrs)
  end

  @doc """
  Ends a time session

  ## Examples

      iex> end_time_session(time_session)
      {:ok, %TimeSession{}}
  """
  def end_time_session(%TimeSession{} = time_session) do
    update_time_session(time_session, %{end_time: DateTime.utc_now()})
  end

  @doc """
  Marks a time session as interrupted and points to the task id that interrupted it

  ## Examples

      iex> interrupt_time_session(time_session, 123)
      {:ok, %TimeSession{}}
  """
  def interrupt_time_session(%TimeSession{} = time_session, interrupted_by_task_id) do
    time_session
    |> TimeSession.interrupt(interrupted_by_task_id)
    |> Repo.update()
  end

  @doc """
  Returns the first active TimeSession if there is one

  ## Examples

      iex> get_active_time_session()
      %TimeSession{}

      iex> get_active_time_session()
      ** (%Ecto.NoResultsError)
  """
  def get_active_time_session do
    TimeSession
    |> where([ts], is_nil(ts.end_time))
    |> limit(1)
    |> Repo.one()
  end

  @doc """
  Returns the total time spent on a task summarizing all sessions in seconds

  ## Examples

      iex> get_total_time_by_task(123)
      3600

  """
  def get_total_time_by_task(task_id) do
    query =
      from ts in TimeSession,
        where: ts.task_id == ^task_id and not is_nil(ts.end_time),
        select:
          sum(
            fragment("EXTRACT(EPOCH FROM ?) - EXTRACT(EPOCH FROM ?)", ts.end_time, ts.start_time)
          )

    (Repo.one(query) || Decimal.new("0"))
    |> Decimal.to_float()
  end

  @doc """
  Returns the total time spent on a task summarizing all sessions in minutes

  ## Examples

      iex> get_total_minutes_by_task(123)
      60

  """
  def get_total_minutes_by_task(task_id) do
    get_total_time_by_task(task_id) / 60
  end

  @doc """
  Return total seconds from time_sessions that ended

  ## Examples

      iex> get_total_minutes_from_task(get_task_with_sessions!(123))
      60

  """
  def get_total_seconds_from_task(%{time_sessions: %Ecto.Association.NotLoaded{}}), do: 0

  def get_total_seconds_from_task(task) do
    task.time_sessions
    |> Enum.filter(fn session -> session.end_time != nil end)
    |> Enum.map(fn session ->
      DateTime.diff(session.end_time, session.start_time, :second)
    end)
    |> Enum.sum()
  end
end
