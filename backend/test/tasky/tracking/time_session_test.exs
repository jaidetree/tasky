defmodule Tasky.Tracking.TimeSessionTest do
  use Tasky.DataCase

  alias Tasky.Tracking.TimeSession

  describe "changeset" do
    @valid_attrs %{
      start_time: DateTime.utc_now(),
      task_id: Ecto.UUID.generate()
    }
    @invalid_attrs %{start_time: nil, task_id: nil}

    test "changeset with valid attributes" do
      changeset = TimeSession.changeset(%TimeSession{}, @valid_attrs)
      assert changeset.valid?
    end

    test "changeset with invalid attributes" do
      changeset = TimeSession.changeset(%TimeSession{}, @invalid_attrs)
      refute changeset.valid?
    end

    test "changeset requires start_time" do
      attrs = Map.delete(@valid_attrs, :start_time)
      changeset = TimeSession.changeset(%TimeSession{}, attrs)
      assert %{start_time: ["can't be blank"]} = errors_on(changeset)
    end

    test "changeset requires task_id" do
      attrs = Map.delete(@valid_attrs, :task_id)
      changeset = TimeSession.changeset(%TimeSession{}, attrs)
      assert %{task_id: ["can't be blank"]} = errors_on(changeset)
    end

    test "validates end_time is after start_time" do
      start_time = DateTime.utc_now()
      end_time = DateTime.add(start_time, -3600, :second)

      attrs = Map.merge(@valid_attrs, %{start_time: start_time, end_time: end_time})
      changeset = TimeSession.changeset(%TimeSession{}, attrs)

      assert %{end_time: ["must be after start time"]} = errors_on(changeset)
    end

    test "accepts valid end_time" do
      start_time = DateTime.utc_now()
      end_time = DateTime.add(start_time, 3600, :second)

      attrs = Map.merge(@valid_attrs, %{start_time: start_time, end_time: end_time})
      changeset = TimeSession.changeset(%TimeSession{}, attrs)

      assert changeset.valid?
    end
  end

  describe "interrupt/2" do
    test "sets end_time and interrupted_by_task_id" do
      start_time = DateTime.utc_now()

      time_session = %TimeSession{
        id: Ecto.UUID.generate(),
        start_time: start_time,
        task_id: Ecto.UUID.generate()
      }

      interrupted_by_task_id = Ecto.UUID.generate()
      changeset = TimeSession.interrupt(time_session, interrupted_by_task_id)

      assert %DateTime{} = changeset.changes.end_time
      assert changeset.changes.interrupted_by_task_id == interrupted_by_task_id
    end

    test "cannot interrupt a session that has already ended" do
      start_time = DateTime.utc_now() |> DateTime.add(-3600, :second)
      end_time = DateTime.utc_now() |> DateTime.add(-1800, :second)

      time_session = %TimeSession{
        id: Ecto.UUID.generate(),
        start_time: start_time,
        end_time: end_time,
        task_id: Ecto.UUID.generate()
      }

      interrupted_by_task_id = Ecto.UUID.generate()
      changeset = TimeSession.interrupt(time_session, interrupted_by_task_id)

      refute changeset.valid?
      assert {"cannot interrupt a completed session", _} = changeset.errors[:end_time]
    end
  end

  describe "duration_minutes/1" do
    test "calculates duration when both start and end times exist" do
      start_time = DateTime.utc_now()
      # 1 hour later
      end_time = DateTime.add(start_time, 3600, :second)

      time_session = %TimeSession{
        start_time: start_time,
        end_time: end_time
      }

      assert TimeSession.duration_minutes(time_session) == 60.0
    end

    test "calculates current duration when end_time is nil" do
      # 10 minutes ago
      start_time = DateTime.add(DateTime.utc_now(), -600, :second)

      time_session = %TimeSession{
        start_time: start_time,
        end_time: nil
      }

      # Approximately 10 minutes
      duration = TimeSession.duration_minutes(time_session)
      assert duration >= 9.9 && duration <= 10.1
    end

    test "returns 0 for empty start_time" do
      time_session = %TimeSession{
        start_time: nil,
        end_time: nil
      }

      assert TimeSession.duration_minutes(time_session) == 0
    end
  end
end
