defmodule Tasky.Repo.Migrations.CreateNotes do
  use Ecto.Migration

  def change do
    create table(:notes, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :content, :text, null: false
      add :sort_order, :integer, null: false, default: 0
      add :deleted_at, :utc_datetime
      add :parent_note_id, references(:notes, on_delete: :delete_all, type: :binary_id)
      add :task_id, references(:tasks, on_delete: :delete_all, type: :binary_id)

      timestamps(type: :utc_datetime)
    end

    create index(:notes, [:parent_note_id])
    create index(:notes, [:task_id])
  end
end
