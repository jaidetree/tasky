defmodule Tasky.Repo.Migrations.CreateTimeSessions do
  use Ecto.Migration

  def change do
    create table(:time_sessions, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :start_time, :utc_datetime
      add :end_time, :utc_datetime
      add :original_end_time, :utc_datetime
      add :notes, :text, null: false, default: ""
      add :task_id, references(:tasks, on_delete: :delete_all, type: :binary_id)
      add :interrupted_by_task_id, references(:tasks, on_delete: :delete_all, type: :binary_id)

      timestamps(type: :utc_datetime)
    end

    create index(:time_sessions, [:task_id])
    create index(:time_sessions, [:interrupted_by_task_id])
  end
end
