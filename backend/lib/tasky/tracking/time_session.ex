defmodule Tasky.Tracking.TimeSession do
  use Ecto.Schema
  import Ecto.Changeset
  alias Tasky.Tracking.Task
  alias Tasky.Utils.DateTimeUtils

  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id
  schema "time_sessions" do
    field :start_time, :utc_datetime
    field :end_time, :utc_datetime
    field :original_end_time, :utc_datetime
    field :notes, :string

    belongs_to :task, Task
    belongs_to :interrupted_by_task, Task

    timestamps(type: :utc_datetime)
  end

  @doc false
  def changeset(time_session, attrs) do
    time_session
    |> cast(attrs, [
      :start_time,
      :end_time,
      :original_end_time,
      :notes,
      :task_id,
      :interrupted_by_task_id
    ])
    |> validate_required([:start_time, :task_id])
    |> validate_end_time()
    |> foreign_key_constraint(:task_id)
    |> foreign_key_constraint(:interrupted_by_task_id)
  end

  # Ensure end_time is after start_time if both ar provided
  defp validate_end_time(changeset) do
    case {get_field(changeset, :start_time), get_field(changeset, :end_time)} do
      {start_time, end_time} when not is_nil(start_time) and not is_nil(end_time) ->
        if DateTime.compare(start_time, end_time) == :lt do
          changeset
        else
          add_error(changeset, :end_time, "must be after start time")
        end

      _ ->
        changeset
    end
  end

  def now do
    DateTime.utc_now()
    |> DateTime.truncate(:second)
  end

  def interrupt(time_session, interrupted_by_task_id, end_time \\ DateTimeUtils.now()) do
    # Only allow interrupting active sessions (those without an end_time)
    if time_session.end_time do
      # Return the changeset with an error if session is already ended
      change(time_session)
      |> add_error(:end_time, "cannot interrupt a completed session")
    else
      # Set end_time and interrupted_by_task_id for active sessions
      change(time_session, %{
        end_time: end_time,
        interrupted_by_task_id: interrupted_by_task_id
      })
    end
  end

  def duration_minutes(time_session) do
    case {time_session.start_time, time_session.end_time} do
      {start_time, end_time} when not is_nil(start_time) and not is_nil(end_time) ->
        DateTime.diff(end_time, start_time, :minute)

      {start_time, nil} when not is_nil(start_time) ->
        DateTime.diff(DateTime.utc_now(), start_time, :minute)

      _ ->
        0
    end
  end
end
