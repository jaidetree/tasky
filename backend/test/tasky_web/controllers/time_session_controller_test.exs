defmodule TaskyWeb.TimeSessionControllerTest do
  use TaskyWeb.ConnCase

  import Tasky.TrackingFixtures

  alias Tasky.Tracking.TimeSession

  @create_attrs %{
    start_time: ~U[2023-01-01 12:00:00Z],
    notes: "some notes"
  }
  @update_attrs %{
    end_time: ~U[2023-01-01 13:00:00Z],
    notes: "updated notes"
  }
  @invalid_attrs %{start_time: nil, task_id: nil}

  setup %{conn: conn} do
    task = task_fixture()
    {:ok, conn: put_req_header(conn, "accept", "application/json"), task: task}
  end

  describe "index" do
    test "lists all time_sessions", %{conn: conn, task: task} do
      time_session_fixture(%{task_id: task.id})
      conn = get(conn, ~p"/api/time_sessions")
      assert json_response(conn, 200)["data"] |> length() > 0
    end
  end

  describe "create time_session" do
    test "renders time_session when data is valid", %{conn: conn, task: task} do
      attrs = Map.put(@create_attrs, :task_id, task.id)
      conn = post(conn, ~p"/api/time_sessions", time_session: attrs)
      assert %{"id" => id} = json_response(conn, 201)["data"]

      conn = get(conn, ~p"/api/time_sessions/#{id}")

      assert %{
               "id" => ^id,
               "notes" => "some notes",
               "task_id" => task_id
             } = json_response(conn, 200)["data"]

      assert task_id == task.id
    end

    test "renders errors when data is invalid", %{conn: conn} do
      conn = post(conn, ~p"/api/time_sessions", time_session: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "update time_session" do
    setup [:create_time_session]

    test "renders time_session when data is valid", %{
      conn: conn,
      time_session: %TimeSession{id: id} = time_session
    } do
      conn = put(conn, ~p"/api/time_sessions/#{time_session}", time_session: @update_attrs)
      assert %{"id" => ^id} = json_response(conn, 200)["data"]

      conn = get(conn, ~p"/api/time_sessions/#{id}")

      assert %{
               "id" => ^id,
               "notes" => "updated notes",
               "end_time" => "2023-01-01T13:00:00Z"
             } = json_response(conn, 200)["data"]
    end

    test "renders errors when data is invalid", %{conn: conn, time_session: time_session} do
      conn = put(conn, ~p"/api/time_sessions/#{time_session}", time_session: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "delete time_session" do
    setup [:create_time_session]

    test "deletes chosen time_session", %{conn: conn, time_session: time_session} do
      conn = delete(conn, ~p"/api/time_sessions/#{time_session}")
      assert response(conn, 204)

      assert_error_sent 404, fn ->
        get(conn, ~p"/api/time_sessions/#{time_session}")
      end
    end
  end

  describe "end time_session" do
    setup [:create_time_session]

    test "ends a time session", %{conn: conn, time_session: time_session} do
      conn = post(conn, ~p"/api/time_sessions/#{time_session.id}/end")
      assert %{"id" => id, "end_time" => end_time} = json_response(conn, 200)["data"]
      assert id == time_session.id
      assert end_time != nil
    end
  end

  describe "interrupt time_session" do
    setup [:create_time_session]

    test "interrupts a time session", %{conn: conn, time_session: time_session, task: _task} do
      interrupting_task = task_fixture(%{title: "Interrupting Task"})

      conn =
        post(conn, ~p"/api/time_sessions/#{time_session.id}/interrupt",
          interrupted_by_task_id: interrupting_task.id
        )

      assert %{
               "id" => id,
               "end_time" => end_time,
               "interrupted_by_task_id" => interrupted_by_task_id
             } = json_response(conn, 200)["data"]

      assert id == time_session.id
      assert end_time != nil
      assert interrupted_by_task_id == interrupting_task.id
    end
  end

  describe "active time_session" do
    test "gets active time session", %{conn: conn, task: task} do
      # Create an active session
      {:ok, time_session} = Tasky.Tracking.start_time_session(task.id)

      conn = get(conn, ~p"/api/time_sessions/active")
      assert %{"id" => id} = json_response(conn, 200)["data"]
      assert id == time_session.id
    end

    test "returns not found when no active session", %{conn: conn} do
      conn = get(conn, ~p"/api/time_sessions/active")
      assert json_response(conn, 404)["message"] == "No active time session found"
    end
  end

  defp create_time_session(%{task: task}) do
    time_session = time_session_fixture(Map.put(@create_attrs, :task_id, task.id))
    %{time_session: time_session}
  end
end
