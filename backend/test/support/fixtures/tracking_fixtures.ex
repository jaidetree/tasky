defmodule Tasky.TrackingFixtures do
  @moduledoc """
  This module defines test helpers for creating
  entities via the `Tasky.Tracking` context.
  """

  @doc """
  Generate a task.
  """
  def task_fixture(attrs \\ %{}) do
    {:ok, task} =
      attrs
      |> Enum.into(%{
        title: "Task #{System.unique_integer([:positive])}",
        estimated_time: 60,
        notes: "some notes"
      })
      |> Tasky.Tracking.create_task()

    task
  end

  @doc """
  Generate a time_session.
  """
  def time_session_fixture(attrs \\ %{}) do
    task = attrs[:task] || task_fixture()

    {:ok, time_session} =
      attrs
      |> Enum.into(%{
        notes: "some notes",
        start_time: ~U[2025-02-24 07:06:00Z],
        task_id: task.id
      })
      |> Tasky.Tracking.create_time_session()

    time_session
  end
end
