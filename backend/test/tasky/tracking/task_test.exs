defmodule Tasky.Tracking.TaskTest do
  use Tasky.DataCase

  alias Tasky.Tracking.Task

  describe "changeset" do
    @valid_attrs %{title: "Task 1", estimated_time: 60}
    @invalid_attrs %{title: nil, estimated_time: nil}

    test "changeset with valid attributes" do
      changeset = Task.changeset(%Task{}, @valid_attrs)
      assert changeset.valid?
    end

    test "changeset with invalid attributes" do
      changeset = Task.changeset(%Task{}, @invalid_attrs)
      refute changeset.valid?
    end

    test "changeset requires title" do
      attrs = Map.delete(@valid_attrs, :title)
      changeset = Task.changeset(%Task{}, attrs)
      assert %{title: ["can't be blank"]} = errors_on(changeset)
    end

    test "changeset requires estimated_time" do
      attrs = Map.delete(@valid_attrs, :estimated_time)
      changeset = Task.changeset(%Task{}, attrs)
      assert %{estimated_time: ["can't be blank"]} = errors_on(changeset)
    end

    test "changeset validates estimated_time is positive" do
      attrs = Map.put(@valid_attrs, :estimated_time, 0)
      changeset = Task.changeset(%Task{}, attrs)
      assert %{estimated_time: ["must be greater than 0"]} = errors_on(changeset)

      attrs = Map.put(@valid_attrs, :estimated_time, -10)
      changeset = Task.changeset(%Task{}, attrs)
      assert %{estimated_time: ["must be greater than 0"]} = errors_on(changeset)
    end
  end

  describe "status functions" do
    setup do
      {:ok, task: %Task{id: Ecto.UUID.generate(), title: "Test Task", estimated_time: 60}}
    end

    test "complete/1 sets completed_at", %{task: task} do
      changeset = Task.complete(task)
      assert %DateTime{} = changeset.changes.completed_at
    end

    test "incomplete/1 clears completed_at", %{task: task} do
      task = %{task | completed_at: DateTime.utc_now()}
      changeset = Task.incomplete(task)
      assert changeset.changes.completed_at == nil
    end

    test "soft_delete/1 sets deleted_at", %{task: task} do
      changeset = Task.soft_delete(task)
      assert %DateTime{} = changeset.changes.deleted_at
    end

    test "restore/1 clears deleted_at", %{task: task} do
      task = %{task | deleted_at: DateTime.utc_now()}
      changeset = Task.restore(task)
      assert changeset.changes.deleted_at == nil
    end
  end
end
