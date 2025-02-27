defmodule Tasky.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      TaskyWeb.Telemetry,
      Tasky.Repo,
      {DNSCluster, query: Application.get_env(:tasky, :dns_cluster_query) || :ignore},
      {Phoenix.PubSub, name: Tasky.PubSub},
      # Start the Finch HTTP client for sending emails
      {Finch, name: Tasky.Finch},
      # Start a worker by calling: Tasky.Worker.start_link(arg)
      # {Tasky.Worker, arg},
      # Start to serve requests, typically the last entry
      TaskyWeb.Endpoint
    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Tasky.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  @impl true
  def config_change(changed, _new, removed) do
    TaskyWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
