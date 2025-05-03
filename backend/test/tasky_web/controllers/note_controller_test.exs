defmodule TaskyWeb.NoteControllerTest do
  use TaskyWeb.ConnCase

  import Tasky.NotesFixtures

  alias Tasky.Notes.Note

  @create_attrs %{
    content: "some content",
    sort_order: 42,
    deleted_at: ~U[2025-05-02 02:23:00Z]
  }
  @update_attrs %{
    content: "some updated content",
    sort_order: 43,
    deleted_at: ~U[2025-05-03 02:23:00Z]
  }
  @invalid_attrs %{content: nil, sort_order: nil, deleted_at: nil}

  setup %{conn: conn} do
    {:ok, conn: put_req_header(conn, "accept", "application/json")}
  end

  describe "index" do
    test "lists all notes", %{conn: conn} do
      conn = get(conn, ~p"/api/notes")
      assert json_response(conn, 200)["data"] == []
    end
  end

  describe "create note" do
    test "renders note when data is valid", %{conn: conn} do
      conn = post(conn, ~p"/api/notes", note: @create_attrs)
      assert %{"id" => id} = json_response(conn, 201)["data"]

      conn = get(conn, ~p"/api/notes/#{id}")

      assert %{
               "id" => ^id,
               "content" => "some content",
               "deleted_at" => "2025-05-02T02:23:00Z",
               "sort_order" => 42
             } = json_response(conn, 200)["data"]
    end

    test "renders errors when data is invalid", %{conn: conn} do
      conn = post(conn, ~p"/api/notes", note: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "update note" do
    setup [:create_note]

    test "renders note when data is valid", %{conn: conn, note: %Note{id: id} = note} do
      conn = put(conn, ~p"/api/notes/#{note}", note: @update_attrs)
      assert %{"id" => ^id} = json_response(conn, 200)["data"]

      conn = get(conn, ~p"/api/notes/#{id}")

      assert %{
               "id" => ^id,
               "content" => "some updated content",
               "deleted_at" => "2025-05-03T02:23:00Z",
               "sort_order" => 43
             } = json_response(conn, 200)["data"]
    end

    test "renders errors when data is invalid", %{conn: conn, note: note} do
      conn = put(conn, ~p"/api/notes/#{note}", note: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "delete note" do
    setup [:create_note]

    test "deletes chosen note", %{conn: conn, note: note} do
      conn = delete(conn, ~p"/api/notes/#{note}")
      assert response(conn, 204)

      assert_error_sent 404, fn ->
        get(conn, ~p"/api/notes/#{note}")
      end
    end
  end

  defp create_note(_) do
    note = note_fixture()
    %{note: note}
  end
end
