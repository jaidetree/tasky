defmodule Tasky.Repo do
  use Ecto.Repo,
    otp_app: :tasky,
    adapter: Ecto.Adapters.Postgres
end
