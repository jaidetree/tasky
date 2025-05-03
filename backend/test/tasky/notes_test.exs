defmodule Tasky.NotesTest do
  use Tasky.DataCase

  alias Tasky.Notes

  describe "notes" do
    alias Tasky.Notes.Note

    import Tasky.NotesFixtures

    @invalid_attrs %{content: nil, sort_order: nil, deleted_at: nil}

    test "list_notes/0 returns all notes" do
      note = note_fixture()
      assert Notes.list_notes() == [note]
    end

    test "get_note!/1 returns the note with given id" do
      note = note_fixture()
      assert Notes.get_note!(note.id) == note
    end

    test "create_note/1 with valid data creates a note" do
      valid_attrs = %{content: "some content", sort_order: 42, deleted_at: ~U[2025-05-02 02:23:00Z]}

      assert {:ok, %Note{} = note} = Notes.create_note(valid_attrs)
      assert note.content == "some content"
      assert note.sort_order == 42
      assert note.deleted_at == ~U[2025-05-02 02:23:00Z]
    end

    test "create_note/1 with invalid data returns error changeset" do
      assert {:error, %Ecto.Changeset{}} = Notes.create_note(@invalid_attrs)
    end

    test "update_note/2 with valid data updates the note" do
      note = note_fixture()
      update_attrs = %{content: "some updated content", sort_order: 43, deleted_at: ~U[2025-05-03 02:23:00Z]}

      assert {:ok, %Note{} = note} = Notes.update_note(note, update_attrs)
      assert note.content == "some updated content"
      assert note.sort_order == 43
      assert note.deleted_at == ~U[2025-05-03 02:23:00Z]
    end

    test "update_note/2 with invalid data returns error changeset" do
      note = note_fixture()
      assert {:error, %Ecto.Changeset{}} = Notes.update_note(note, @invalid_attrs)
      assert note == Notes.get_note!(note.id)
    end

    test "delete_note/1 deletes the note" do
      note = note_fixture()
      assert {:ok, %Note{}} = Notes.delete_note(note)
      assert_raise Ecto.NoResultsError, fn -> Notes.get_note!(note.id) end
    end

    test "change_note/1 returns a note changeset" do
      note = note_fixture()
      assert %Ecto.Changeset{} = Notes.change_note(note)
    end
  end
end
