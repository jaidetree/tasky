defmodule Tasky.Notes.Note do
  use Ecto.Schema
  import Ecto.Changeset
  alias Tasky.Tracking.Task

  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id
  schema "notes" do
    field :content, :string, default: ""
    field :sort_order, :integer
    field :deleted_at, :utc_datetime
    field :parent_note_id, :binary_id
    belongs_to :task, Task

    timestamps(type: :utc_datetime)
  end

  @doc false
  def changeset(note, attrs) do
    note
    |> cast(attrs, [:content, :sort_order, :deleted_at])
    |> validate_required([:content, :sort_order, :deleted_at])
  end
end
