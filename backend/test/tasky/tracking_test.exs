defmodule Tasky.TrackingTest do
  use Tasky.DataCase

  alias Tasky.Tracking

  describe "tasks" do
    alias Tasky.Tracking.Task

    import Tasky.TrackingFixtures

    @invalid_attrs %{
      title: nil,
      notes: nil,
      estimated_time: nil,
      due_date: nil,
      completed_at: nil,
      deleted_at: nil
    }

    test "list_tasks/0 returns all tasks" do
      task = task_fixture()
      assert Tracking.list_tasks() == [task]
    end

    test "get_task!/1 returns the task with given id" do
      task = task_fixture()
      assert Tracking.get_task!(task.id) == task
    end

    test "create_task/1 with valid data creates a task" do
      valid_attrs = %{
        title: "some title",
        notes: "some notes",
        estimated_time: 42,
        due_date: ~U[2025-02-24 07:06:00Z],
        completed_at: ~U[2025-02-24 07:06:00Z],
        deleted_at: ~U[2025-02-24 07:06:00Z]
      }

      assert {:ok, %Task{} = task} = Tracking.create_task(valid_attrs)
      assert task.title == "some title"
      assert task.notes == "some notes"
      assert task.estimated_time == 42
      assert task.due_date == ~U[2025-02-24 07:06:00Z]
      assert task.completed_at == ~U[2025-02-24 07:06:00Z]
      assert task.deleted_at == ~U[2025-02-24 07:06:00Z]
    end

    test "create_task/1 with invalid data returns error changeset" do
      assert {:error, %Ecto.Changeset{}} = Tracking.create_task(@invalid_attrs)
    end

    test "update_task/2 with valid data updates the task" do
      task = task_fixture()

      update_attrs = %{
        title: "some updated title",
        notes: "some updated notes",
        estimated_time: 43,
        due_date: ~U[2025-02-25 07:06:00Z],
        completed_at: ~U[2025-02-25 07:06:00Z],
        deleted_at: ~U[2025-02-25 07:06:00Z]
      }

      assert {:ok, %Task{} = task} = Tracking.update_task(task, update_attrs)
      assert task.title == "some updated title"
      assert task.notes == "some updated notes"
      assert task.estimated_time == 43
      assert task.due_date == ~U[2025-02-25 07:06:00Z]
      assert task.completed_at == ~U[2025-02-25 07:06:00Z]
      assert task.deleted_at == ~U[2025-02-25 07:06:00Z]
    end

    test "update_task/2 with invalid data returns error changeset" do
      task = task_fixture()
      assert {:error, %Ecto.Changeset{}} = Tracking.update_task(task, @invalid_attrs)
      assert task == Tracking.get_task!(task.id)
    end

    test "delete_task/1 soft deletes the task" do
      task = task_fixture()
      assert is_nil(task.deleted_at)
      assert {:ok, %Task{} = task} = Tracking.delete_task(task)
      assert not is_nil(task.deleted_at)
    end

    test "change_task/1 returns a task changeset" do
      task = task_fixture()
      assert %Ecto.Changeset{} = Tracking.change_task(task)
    end

    test "complete_task/1 marks a task as completed" do
      task = task_fixture()
      assert is_nil(task.completed_at)

      assert {:ok, %Task{} = completed_task} = Tracking.complete_task(task)
      assert not is_nil(completed_task.completed_at)
    end

    test "incomplete_task/1 marks a task as not completed" do
      task = task_fixture()
      {:ok, completed_task} = Tracking.complete_task(task)

      assert {:ok, %Task{} = task} = Tracking.incomplete_task(completed_task)
      assert is_nil(task.completed_at)
    end
  end

  describe "time_sessions" do
    alias Tasky.Tracking.TimeSession

    import Tasky.TrackingFixtures

    @invalid_attrs %{start_time: nil, end_time: nil, original_end_time: nil, notes: nil}

    test "list_time_sessions/0 returns all time_sessions" do
      time_session = time_session_fixture()
      assert Tracking.list_time_sessions() == [time_session]
    end

    test "get_time_session!/1 returns the time_session with given id" do
      time_session = time_session_fixture()
      assert Tracking.get_time_session!(time_session.id) == time_session
    end

    test "create_time_session/1 with valid data creates a time_session" do
      task = task_fixture()

      valid_attrs = %{
        start_time: ~U[2025-02-24 07:06:00Z],
        end_time: ~U[2025-02-24 07:08:00Z],
        original_end_time: ~U[2025-02-24 07:06:00Z],
        notes: "some notes",
        task_id: task.id
      }

      assert {:ok, %TimeSession{} = time_session} = Tracking.create_time_session(valid_attrs)
      assert time_session.start_time == ~U[2025-02-24 07:06:00Z]
      assert time_session.end_time == ~U[2025-02-24 07:08:00Z]
      assert time_session.original_end_time == ~U[2025-02-24 07:06:00Z]
      assert time_session.notes == "some notes"
    end

    test "create_time_session/1 with invalid data returns error changeset" do
      assert {:error, %Ecto.Changeset{}} = Tracking.create_time_session(@invalid_attrs)
    end

    test "update_time_session/2 with valid data updates the time_session" do
      time_session = time_session_fixture()

      update_attrs = %{
        start_time: ~U[2025-02-25 07:06:00Z],
        end_time: ~U[2025-02-25 07:07:00Z],
        original_end_time: ~U[2025-02-25 07:06:00Z],
        notes: "some updated notes"
      }

      assert {:ok, %TimeSession{} = time_session} =
               Tracking.update_time_session(time_session, update_attrs)

      assert time_session.start_time == ~U[2025-02-25 07:06:00Z]
      assert time_session.end_time == ~U[2025-02-25 07:07:00Z]
      assert time_session.original_end_time == ~U[2025-02-25 07:06:00Z]
      assert time_session.notes == "some updated notes"
    end

    test "update_time_session/2 with invalid data returns error changeset" do
      time_session = time_session_fixture()

      assert {:error, %Ecto.Changeset{}} =
               Tracking.update_time_session(time_session, @invalid_attrs)

      assert time_session == Tracking.get_time_session!(time_session.id)
    end

    test "delete_time_session/1 deletes the time_session" do
      time_session = time_session_fixture()
      assert {:ok, %TimeSession{}} = Tracking.delete_time_session(time_session)
      assert_raise Ecto.NoResultsError, fn -> Tracking.get_time_session!(time_session.id) end
    end

    test "change_time_session/1 returns a time_session changeset" do
      time_session = time_session_fixture()
      assert %Ecto.Changeset{} = Tracking.change_time_session(time_session)
    end

    test "start_time_session/1 creates a new time session with current time" do
      task = task_fixture()

      assert {:ok, %TimeSession{} = time_session} = Tracking.start_time_session(task.id)
      assert time_session.task_id == task.id
      assert not is_nil(time_session.start_time)
      assert is_nil(time_session.end_time)
    end

    test "end_time_session/1 sets the end time" do
      time_session = time_session_fixture()

      assert {:ok, %TimeSession{} = updated} = Tracking.end_time_session(time_session)
      assert not is_nil(updated.end_time)
      assert DateTime.compare(updated.end_time, updated.start_time) == :gt
    end

    test "interrupt_time_session/2 interrupts a session" do
      time_session = time_session_fixture()
      interrupting_task = task_fixture(%{title: "Interrupting Task"})

      assert {:ok, %TimeSession{} = interrupted} =
               Tracking.interrupt_time_session(time_session, interrupting_task.id)

      assert interrupted.interrupted_by_task_id == interrupting_task.id
      assert not is_nil(interrupted.end_time)
    end

    test "get_active_time_session/0 returns session without end_time" do
      # Create a completed session
      time_session = time_session_fixture()
      {:ok, _} = Tracking.end_time_session(time_session)

      # Create an active session
      task = task_fixture(%{title: "Active Task"})
      {:ok, active_session} = Tracking.start_time_session(task.id)

      # Verify we get the active one
      assert Tracking.get_active_time_session().id == active_session.id
    end

    test "get_total_time_by_task/1 returns total duration" do
      task = task_fixture()

      # Session 1: 30 minutes
      start1 = DateTime.utc_now() |> DateTime.add(-60, :minute)
      end1 = start1 |> DateTime.add(30, :minute)

      {:ok, _} =
        Tracking.create_time_session(%{
          task_id: task.id,
          start_time: start1,
          end_time: end1
        })

      # Session 2: 15 minutes
      start2 = DateTime.utc_now() |> DateTime.add(-30, :minute)
      end2 = start2 |> DateTime.add(15, :minute)

      {:ok, _} =
        Tracking.create_time_session(%{
          task_id: task.id,
          start_time: start2,
          end_time: end2
        })

      # Total should be 45 minutes
      total_seconds = Tracking.get_total_time_by_task(task.id)
      assert_in_delta total_seconds, 2700, 0.1

      # Test the get_total_minutes_by_task function directly
      total_minutes_direct = Tracking.get_total_minutes_by_task(task.id)
      assert_in_delta total_minutes_direct, 45, 0.1
    end

    test "create_time_session/1 interrupts active session when creating a new one", %{} do
      # Create a first task
      first_task = task_fixture(%{title: "First Task"})

      # Start a time session for the first task
      {:ok, first_session} = Tracking.start_time_session(first_task.id)

      # Verify it's active
      assert is_nil(first_session.end_time)
      assert first_session.task_id == first_task.id

      # Create a second task
      second_task = task_fixture(%{title: "Second Task"})

      # Start a time session for the second task - this should interrupt the first
      {:ok, second_session} = Tracking.start_time_session(second_task.id)

      # Verify the second session is active
      assert is_nil(second_session.end_time)
      assert second_session.task_id == second_task.id

      # Reload the first session to see if it was interrupted
      updated_first_session = Tracking.get_time_session!(first_session.id)

      # Verify the first session was interrupted
      refute is_nil(updated_first_session.end_time)
      assert updated_first_session.interrupted_by_task_id == second_task.id

      # Verify there is only one active session
      active_session = Tracking.get_active_time_session()
      assert active_session.id == second_session.id
    end
  end
end
