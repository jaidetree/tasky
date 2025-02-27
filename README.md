# Tasky

A time tracking tool built with Elixir/Phoenix and ReScript/Preact.

## Prerequisites

- [Nix](https://nixos.org/download.html) with flakes enabled
- [direnv](https://direnv.net/) (recommended)

## Project Structure

```
.
├── backend/         # Phoenix API
├── frontend/        # ReScript/Preact UI
├── scripts/        # Development utilities
├── .envrc          # Environment configuration
└── flake.nix       # Nix development environment
```

## Setup

1. Clone the repository:

```bash
git clone <repository-url>
cd tasky
```

2. Allow direnv:

```bash
direnv allow
```

3. Initialize PostgreSQL:

```bash
manage-postgres.sh init
manage-postgres.sh start
```

4. Set up the backend:

```bash
cd backend
mix deps.get
mix ecto.setup
iex -S mix phx.serve
mix phx.server # OR
```

5. Set up the frontend:

```bash
cd frontend
npm install
npm run res:dev  # In one terminal
npm run dev      # In another terminal
```

## Development

### Database Management

The PostgreSQL server can be managed using the provided script:

```bash
manage-postgres.sh start   # Start the server
manage-postgres.sh stop    # Stop the server
manage-postgres.sh status  # Check server status
```

### Running the Application

1. Start PostgreSQL:

```bash
manage-postgres.sh start
```

2. Start the Phoenix server:

```bash
cd backend
mix phx.server
```

3. Start the frontend development server:

```bash
cd frontend
npm run dev
```

The application will be available at:

- Frontend: http://localhost:3000
- Backend API: http://localhost:4000

## Environment Variables

The following environment variables are configured in `.envrc`:

```bash
PGDATA="$PWD/postgres_data"
PGHOST="$PGDATA"
PGPORT=5432
PGDATABASE="tasky_dev"
```

## Development Tools

The development environment (configured in `flake.nix`) includes:

- Elixir & Erlang
- PostgreSQL 14
- pgcli (enhanced PostgreSQL CLI)
- File watching tools (fswatch on macOS, inotify-tools on Linux)

## Testing

### Backend Testing

The Phoenix backend uses ExUnit for testing. Run the test suite with:

```bash
cd backend
mix test
```

For more detailed test output:

```bash
mix test --trace
```

For test coverage:

```bash
mix test --cover
```

### Frontend Testing

The ReScript frontend can be tested using Jest:

```bash
cd frontend
npm test
```

For testing in watch mode:

```bash
npm test -- --watch
```

## Deployment

TODO: Add deployment instructions
