defmodule Tasky.Tracking.Task do
  use Ecto.Schema
  import Ecto.Changeset
  alias Tasky.Tracking.TimeSession
  alias Tasky.Utils.DateTimeUtils

  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id
  schema "tasks" do
    field :title, :string
    field :notes, :string
    field :estimated_time, :integer
    field :due_date, :utc_datetime
    field :completed_at, :utc_datetime
    field :deleted_at, :utc_datetime

    belongs_to :parent_task, __MODULE__
    has_many :subtasks, __MODULE__, foreign_key: :parent_task_id
    has_many :time_sessions, TimeSession

    timestamps(type: :utc_datetime)
  end

  @doc false
  def changeset(task, attrs) do
    task
    |> cast(attrs, [
      :title,
      :notes,
      :estimated_time,
      :due_date,
      :completed_at,
      :deleted_at,
      :parent_task_id
    ])
    |> validate_required([:title, :estimated_time])
    |> validate_number(:estimated_time, greater_than: 0)
    |> foreign_key_constraint(:parent_task_id)
  end

  def complete(task, completed_at \\ DateTimeUtils.now()) do
    change(task, %{completed_at: completed_at})
  end

  def incomplete(task) do
    change(task, %{completed_at: nil})
  end

  def soft_delete(task, deleted_at \\ DateTimeUtils.now()) do
    change(task, %{deleted_at: deleted_at})
  end

  def restore(task) do
    change(task, %{deleted_at: nil})
  end
end
