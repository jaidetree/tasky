defmodule Tasky.Repo.Migrations.CreateTasks do
  use Ecto.Migration

  def change do
    create table(:tasks, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :title, :string
      add :description, :text, null: false, default: ""
      add :sort_order, :integer, null: false, default: 0
      add :estimated_time, :integer
      add :due_date, :utc_datetime
      add :completed_at, :utc_datetime
      add :deleted_at, :utc_datetime
      add :parent_task_id, references(:tasks, on_delete: :delete_all, type: :binary_id)

      timestamps(type: :utc_datetime)
    end

    create index(:tasks, [:parent_task_id])
  end
end
