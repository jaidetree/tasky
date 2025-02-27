defmodule Tasky.Utils.DateTimeUtils do
  @moduledoc """
  Helper functions for datetime operations.
  """

  @doc """
  Returns the current UTC time with microseconds truncated.
  Useful for consistency with :utc_datetime fields.
  """
  def now do
    DateTime.utc_now()
    |> DateTime.truncate(:second)
  end
end
