# Tasky Backend Notebook

## Server Commands

### Start Server

```sh
mix phx.server
```

## Database

### Rollback all migrations and run them again

```sh
mix ecto.reset
```

### Run migrations

```sh
mix ecto.migrate
```

### Rollback migration

```sh
mix ecto.rollback
```

## Development

### Generate a context for notes associated with a task

#### Arguments

```
mix phx.gen.context context-module schema-module plural-db-table-name field-name:type [args]
```

#### Example

```sh
mix phx.gen.json Notes Note notes content:text parent_note_id:references:notes task_id:references:tasks sort_order:integer deleted_at:utc_datetime  --binary-id
```
