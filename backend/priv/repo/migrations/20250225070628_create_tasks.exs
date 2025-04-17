defmodule Tasky.Repo.Migrations.CreateTasks do
  use Ecto.Migration

  def change do
    create table(:tasks, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :title, :string
      add :notes, :text, null: false, default: ""
      add :estimated_time, :integer
      add :due_date, :utc_datetime
      add :completed_at, :utc_datetime
      add :deleted_at, :utc_datetime
      add :parent_task_id, references(:tasks, on_delete: :nothing, type: :binary_id)

      timestamps(type: :utc_datetime)
    end

    create index(:tasks, [:parent_task_id])
  end
end
