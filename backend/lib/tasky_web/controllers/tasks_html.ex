defmodule TaskyWeb.TasksHTML do
  use TaskyWeb, :html

  embed_templates "tasks_html/*"

  # Helper functions for the templates
  def format_minutes(nil), do: "-"

  def format_minutes(minutes) do
    hours = div(minutes, 60)
    remaining_minutes = rem(minutes, 60)

    cond do
      hours > 0 -> "#{hours}h #{remaining_minutes}m"
      true -> "#{minutes}m"
    end
  end

  def format_date(nil), do: "-"

  def format_date(date) do
    Calendar.strftime(date, "%b %d, %Y")
  end

  def tracked_time(task) do
    case get_total_session_time(task) do
      0 -> "-"
      time -> format_minutes(time)
    end
  end

  def get_total_session_time(task) do
    Tasky.Tracking.get_total_seconds_from_task(task)
  end

  def completed_class(task) do
    if task.completed_at, do: "bg-gray-50", else: ""
  end
end
