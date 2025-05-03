defmodule Tasky.NotesFixtures do
  @moduledoc """
  This module defines test helpers for creating
  entities via the `Tasky.Notes` context.
  """

  @doc """
  Generate a note.
  """
  def note_fixture(attrs \\ %{}) do
    {:ok, note} =
      attrs
      |> Enum.into(%{
        content: "some content",
        deleted_at: ~U[2025-05-02 00:41:00Z]
      })
      |> Tasky.Notes.create_note()

    note
  end

  @doc """
  Generate a note.
  """
  def note_fixture(attrs \\ %{}) do
    {:ok, note} =
      attrs
      |> Enum.into(%{
        content: "some content",
        deleted_at: ~U[2025-05-02 02:23:00Z],
        sort_order: 42
      })
      |> Tasky.Notes.create_note()

    note
  end
end
