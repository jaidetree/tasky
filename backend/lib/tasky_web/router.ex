defmodule TaskyWeb.Router do
  use TaskyWeb, :router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, html: {TaskyWeb.Layouts, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/", TaskyWeb do
    pipe_through :browser

    get "/", TasksController, :index
  end

  # Other scopes may use custom stacks.
  scope "/api", TaskyWeb do
    pipe_through :api

    resources "/tasks", TaskController do
      post "/complete", TaskController, :complete
      post "/incomplete", TaskController, :incomplete
    end

    get "/time_sessions/active", TimeSessionController, :active
    post "/time_sessions/:id/end", TimeSessionController, :end_session
    post "/time_sessions/:id/interrupt", TimeSessionController, :interrupt
    resources "/time_sessions", TimeSessionController, except: [:new, :edit]
  end

  # Enable LiveDashboard and Swoosh mailbox preview in development
  if Application.compile_env(:tasky, :dev_routes) do
    # If you want to use the LiveDashboard in production, you should put
    # it behind authentication and allow only admins to access it.
    # If your application does not have an admins-only section yet,
    # you can use Plug.BasicAuth to set up some basic authentication
    # as long as you are also using SSL (which you should anyway).
    import Phoenix.LiveDashboard.Router

    scope "/dev" do
      pipe_through :browser

      live_dashboard "/dashboard", metrics: TaskyWeb.Telemetry
    end
  end
end
