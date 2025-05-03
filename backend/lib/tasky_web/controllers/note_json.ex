defmodule TaskyWeb.NoteJSON do
  alias Tasky.Notes.Note

  @doc """
  Renders a list of notes.
  """
  def index(%{notes: notes}) do
    %{data: for(note <- notes, do: data(note))}
  end

  @doc """
  Renders a single note.
  """
  def show(%{note: note}) do
    %{data: data(note)}
  end

  defp data(%Note{} = note) do
    %{
      id: note.id,
      content: note.content,
      sort_order: note.sort_order,
      deleted_at: note.deleted_at
    }
  end
end
