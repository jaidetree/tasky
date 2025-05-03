defmodule TaskyWeb.NoteController do
  use TaskyWeb, :controller

  alias Tasky.Notes
  alias Tasky.Notes.Note

  action_fallback TaskyWeb.FallbackController

  def index(conn, _params) do
    notes = Notes.list_notes()
    render(conn, :index, notes: notes)
  end

  def create(conn, %{"note" => note_params}) do
    with {:ok, %Note{} = note} <- Notes.create_note(note_params) do
      conn
      |> put_status(:created)
      |> put_resp_header("location", ~p"/api/tasks/#{note.task}/notes/#{note}")
      |> render(:show, note: note)
    end
  end

  def show(conn, %{"id" => id}) do
    note = Notes.get_note!(id)
    render(conn, :show, note: note)
  end

  def update(conn, %{"id" => id, "note" => note_params}) do
    note = Notes.get_note!(id)

    with {:ok, %Note{} = note} <- Notes.update_note(note, note_params) do
      render(conn, :show, note: note)
    end
  end

  def delete(conn, %{"id" => id}) do
    note = Notes.get_note!(id)

    with {:ok, %Note{}} <- Notes.delete_note(note) do
      send_resp(conn, :no_content, "")
    end
  end
end
