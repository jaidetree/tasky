defmodule TaskyWeb.TaskControllerTest do
  use TaskyWeb.ConnCase

  import Tasky.TrackingFixtures

  alias Tasky.Tracking.Task

  @create_attrs %{
    title: "some title",
    notes: "some notes",
    estimated_time: 60,
    due_date: ~U[2023-01-15 10:00:00Z]
  }
  @update_attrs %{
    title: "updated title",
    notes: "updated notes",
    estimated_time: 120
  }
  @invalid_attrs %{title: nil, estimated_time: nil}

  setup %{conn: conn} do
    {:ok, conn: put_req_header(conn, "accept", "application/json")}
  end

  describe "index" do
    test "lists all tasks", %{conn: conn} do
      conn = get(conn, ~p"/api/tasks")
      assert json_response(conn, 200)["data"] == []

      task_fixture()
      conn = get(conn, ~p"/api/tasks")
      assert json_response(conn, 200)["data"] |> length() == 1
    end
  end

  describe "create task" do
    test "renders task when data is valid", %{conn: conn} do
      conn = post(conn, ~p"/api/tasks", task: @create_attrs)
      assert %{"id" => id} = json_response(conn, 201)["data"]

      conn = get(conn, ~p"/api/tasks/#{id}")

      assert %{
               "id" => ^id,
               "title" => "some title",
               "estimated_time" => 60
             } = json_response(conn, 200)["data"]
    end

    test "renders errors when data is invalid", %{conn: conn} do
      conn = post(conn, ~p"/api/tasks", task: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "update task" do
    setup [:create_task]

    test "renders task when data is valid", %{conn: conn, task: %Task{id: id} = task} do
      conn = put(conn, ~p"/api/tasks/#{task}", task: @update_attrs)
      assert %{"id" => ^id} = json_response(conn, 200)["data"]

      conn = get(conn, ~p"/api/tasks/#{id}")

      assert %{
               "id" => ^id,
               "title" => "updated title"
             } = json_response(conn, 200)["data"]
    end

    test "renders errors when data is invalid", %{conn: conn, task: task} do
      conn = put(conn, ~p"/api/tasks/#{task}", task: @invalid_attrs)
      assert json_response(conn, 422)["errors"] != %{}
    end
  end

  describe "delete task" do
    setup [:create_task]

    test "soft deletes chosen task", %{conn: conn, task: task} do
      conn = delete(conn, ~p"/api/tasks/#{task}")
      assert response(conn, 204)

      # Task should not appear in index
      conn = get(conn, ~p"/api/tasks")
      assert json_response(conn, 200)["data"] == []

      # But should still be directly accessible
      conn = get(conn, ~p"/api/tasks/#{task.id}")
      assert json_response(conn, 200)["data"]["id"] == task.id
    end
  end

  describe "complete task" do
    setup [:create_task]

    test "marks task as completed", %{conn: conn, task: task} do
      conn = post(conn, ~p"/api/tasks/#{task}/complete")
      assert %{"id" => id, "completed_at" => completed_at} = json_response(conn, 200)["data"]
      assert id == task.id
      assert completed_at != nil
    end
  end

  describe "incomplete task" do
    setup [:create_task]

    test "marks task as not completed", %{conn: conn, task: task} do
      # First complete the task
      conn = post(conn, ~p"/api/tasks/#{task}/complete")
      assert %{"completed_at" => completed_at} = json_response(conn, 200)["data"]
      assert completed_at != nil

      # Then mark as incomplete
      conn = post(conn, ~p"/api/tasks/#{task}/incomplete")
      assert %{"id" => id, "completed_at" => completed_at} = json_response(conn, 200)["data"]
      assert id == task.id
      assert completed_at == nil
    end
  end

  defp create_task(_) do
    task = task_fixture()
    %{task: task}
  end
end
